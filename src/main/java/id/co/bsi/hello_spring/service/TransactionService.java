package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.model.TransactionModel;
import id.co.bsi.hello_spring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Page<TransactionModel> getFilteredTransactions(String accountnum, String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromOptionalString(direction.toUpperCase()).orElse(Sort.Direction.ASC), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        List<TransactionModel> allData = transactionRepository.findAllByAccountnum(accountnum);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            allData = allData.stream()
                    .filter(t ->
                            (t.getDescription() != null && t.getDescription().toLowerCase().contains(kw)) ||
                                    (t.getFromTo() != null && t.getFromTo().toLowerCase().contains(kw)) ||
                                    (t.getType() != null && t.getType().toLowerCase().contains(kw)) ||
                                    String.valueOf(t.getAmount()).contains(kw) ||
                                    (t.getDateTime() != null && t.getDateTime().toLowerCase().contains(kw))
                    )
                    .collect(Collectors.toList());
        }

        int start = Math.min((int) pageable.getOffset(), allData.size());
        int end = Math.min((start + pageable.getPageSize()), allData.size());
        List<TransactionModel> pagedData = allData.subList(start, end);

        return new PageImpl<>(pagedData, pageable, allData.size());
    }

    public TransactionModel saveTransaction(TransactionModel transaction) {
        return transactionRepository.save(transaction);
    }

    public Map<String, Integer> getTransactionSummary(String accountnum) {
        List<TransactionModel> transactions = transactionRepository.findAllByAccountnum(accountnum);

        int totalIncome = transactions.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        int totalExpense = transactions.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        return Map.of(
                "totalIncome", totalIncome,
                "totalExpense", totalExpense
        );
    }

    public Map<String, Integer> getTransactionSummaryByMonth(String accountnum, int monthsAgo) {
        List<TransactionModel> transactions = transactionRepository.findAllByAccountnum(accountnum);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (monthsAgo == 0) { // THIS MONTH
            startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            endDate = now; // sampai hari ini
        } else if (monthsAgo == 1) { // LAST MONTH
            LocalDate lastMonthStart = now.minusMonths(1).toLocalDate().withDayOfMonth(1);
            LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());
            startDate = lastMonthStart.atStartOfDay();
            endDate = lastMonthEnd.atTime(23, 59, 59, 999999999);
        } else if (monthsAgo == 3) { // THREE MONTH AGO
            LocalDate threeMonthStart = now.minusMonths(3).toLocalDate().withDayOfMonth(1);
            LocalDate lastMonthEnd = now.minusMonths(1).toLocalDate().withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth());
            startDate = threeMonthStart.atStartOfDay();
            endDate = lastMonthEnd.atTime(23, 59, 59, 999999999);
        } else {
            // Default fallback, should not happen
            startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            endDate = now;
        }

        List<TransactionModel> filtered = transactions.stream()
                .filter(t -> {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(t.getDateTime());
                        return (dateTime.isEqual(startDate) || dateTime.isAfter(startDate)) &&
                                (dateTime.isBefore(endDate) || dateTime.isEqual(endDate));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        int totalIncome = filtered.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        int totalExpense = filtered.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        return Map.of(
                "totalIncome", totalIncome,
                "totalExpense", totalExpense
        );
    }

    public Page<TransactionModel> getFilteredTransactionsCombined(
            String accountnum, String keyword, int page, int size, String sortBy, String direction, String dateStart, String dateEnd
    ) {
        // Validasi kolom sorting
        List<String> validColumns = List.of("id", "dateTime", "type", "fromTo", "description", "amount");
        if (!validColumns.contains(sortBy)) {
            sortBy = "id";
        }

        // Ambil data dari DB
        List<TransactionModel> allData = transactionRepository.findAllByAccountnum(accountnum);

        // Filter tanggal fleksibel
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (dateStart != null && !dateStart.isEmpty()) {
                dateStart = formatFlexibleDate(dateStart);
                start = LocalDate.parse(dateStart).atStartOfDay();
            }
            if (dateEnd != null && !dateEnd.isEmpty()) {
                dateEnd = formatFlexibleDate(dateEnd);
                end = LocalDate.parse(dateEnd).atTime(23, 59, 59, 999999999);
            }
        } catch (Exception e) {
            // Ignore error date parsing
        }

        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

        // Filter keyword dan tanggal
        List<TransactionModel> filtered = allData.stream()
                .filter(t -> {
                    boolean matches = true;
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        matches = (t.getDescription() != null && t.getDescription().toLowerCase().contains(kw)) ||
                                (t.getFromTo() != null && t.getFromTo().toLowerCase().contains(kw)) ||
                                (t.getType() != null && t.getType().toLowerCase().contains(kw)) ||
                                String.valueOf(t.getAmount()).contains(kw) ||
                                (t.getDateTime() != null && t.getDateTime().toLowerCase().contains(kw));
                    }
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(t.getDateTime());
                        if (finalStart != null && dateTime.isBefore(finalStart)) return false;
                        if (finalEnd != null && dateTime.isAfter(finalEnd)) return false;
                    } catch (Exception e) {
                        return false;
                    }
                    return matches;
                })
                .collect(Collectors.toList());

        // Sorting manual
        final String finalSortBy = sortBy;
        final String finalDirection = direction;

        filtered.sort((t1, t2) -> {
            int comparison = 0;
            switch (finalSortBy) {
                case "amount":
                    comparison = Integer.compare(t1.getAmount(), t2.getAmount());
                    break;
                case "dateTime":
                    comparison = LocalDateTime.parse(t1.getDateTime()).compareTo(LocalDateTime.parse(t2.getDateTime()));
                    break;
                case "type":
                    comparison = t1.getType().compareToIgnoreCase(t2.getType());
                    break;
                case "fromTo":
                    comparison = t1.getFromTo().compareToIgnoreCase(t2.getFromTo());
                    break;
                case "description":
                    comparison = t1.getDescription().compareToIgnoreCase(t2.getDescription());
                    break;
                default:
                    comparison = Long.compare(t1.getId(), t2.getId());
                    break;
            }
            return finalDirection.equalsIgnoreCase("asc") ? comparison : -comparison;
        });


        // Pagination
        int startIdx = Math.min(page * size, filtered.size());
        int endIdx = Math.min(startIdx + size, filtered.size());
        List<TransactionModel> pagedData = filtered.subList(startIdx, endIdx);

        return new PageImpl<>(pagedData, PageRequest.of(page, size), filtered.size());
    }

    // Helper: format tanggal fleksibel
    private String formatFlexibleDate(String date) {
        String[] parts = date.split("-");
        String year = parts[0];
        String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String day = parts[2].length() == 1 ? "0" + parts[2] : parts[2];
        return year + "-" + month + "-" + day;
    }




}


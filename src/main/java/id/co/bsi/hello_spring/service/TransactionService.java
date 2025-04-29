package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.model.TransactionModel;
import id.co.bsi.hello_spring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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

    public Map<String, Object> getTransactionSummary(String accountnum) {
        List<TransactionModel> transactions = transactionRepository.findAllByAccountnum(accountnum);

        int totalIncome = transactions.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        int totalExpense = transactions.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToInt(TransactionModel::getAmount)
                .sum();

        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String firstDate = transactions.stream()
                .map(TransactionModel::getDateTime)
                .sorted()
                .findFirst()
                .map(dt -> LocalDateTime.parse(dt, inputFormatter).format(outputFormatter))
                .orElse(null);

        String lastDate = transactions.stream()
                .map(TransactionModel::getDateTime)
                .sorted((d1, d2) -> d2.compareTo(d1))
                .findFirst()
                .map(dt -> LocalDateTime.parse(dt, inputFormatter).format(outputFormatter))
                .orElse(null);

        return Map.of(
                "totalIncome", totalIncome,
                "totalExpense", totalExpense,
                "firstTransactionDate", firstDate,
                "lastTransactionDate", lastDate
        );
    }


    public Map<String, Object> getTransactionSummaryByMonth(String accountnum, int monthsAgo) {
        List<TransactionModel> transactions = transactionRepository.findAllByAccountnum(accountnum);

        LocalDateTime now = LocalDateTime.now();
        LocalDate startDateLocal;
        LocalDate endDateLocal;

        if (monthsAgo == 0) {
            startDateLocal = now.withDayOfMonth(1).toLocalDate();
            endDateLocal = now.toLocalDate();
        } else if (monthsAgo == 1) {
            LocalDate lastMonthStart = now.minusMonths(1).toLocalDate().withDayOfMonth(1);
            LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());
            startDateLocal = lastMonthStart;
            endDateLocal = lastMonthEnd;
        } else if (monthsAgo == 3) {
            LocalDate threeMonthStart = now.minusMonths(3).toLocalDate().withDayOfMonth(1);
            LocalDate lastMonthEnd = now.minusMonths(1).toLocalDate().withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth());
            startDateLocal = threeMonthStart;
            endDateLocal = lastMonthEnd;
        } else {
            startDateLocal = now.withDayOfMonth(1).toLocalDate();
            endDateLocal = now.toLocalDate();
        }

        // Filter transaksi dalam range tersebut
        LocalDateTime startDateTime = startDateLocal.atStartOfDay();
        LocalDateTime endDateTime = endDateLocal.atTime(23, 59, 59, 999999999);

        List<TransactionModel> filtered = transactions.stream()
                .filter(t -> {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(t.getDateTime());
                        return (dateTime.isEqual(startDateTime) || dateTime.isAfter(startDateTime)) &&
                                (dateTime.isBefore(endDateTime) || dateTime.isEqual(endDateTime));
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

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return Map.of(
                "totalIncome", totalIncome,
                "totalExpense", totalExpense,
                "startDate", startDateLocal.format(dateFormatter),
                "lastDate", endDateLocal.format(dateFormatter)
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

        List<TransactionModel> allData = transactionRepository.findAllByAccountnum(accountnum);

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
        }

        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

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


        int startIdx = Math.min(page * size, filtered.size());
        int endIdx = Math.min(startIdx + size, filtered.size());
        List<TransactionModel> pagedData = filtered.subList(startIdx, endIdx);

        return new PageImpl<>(pagedData, PageRequest.of(page, size), filtered.size());
    }

    private String formatFlexibleDate(String date) {
        String[] parts = date.split("-");
        String year = parts[0];
        String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String day = parts[2].length() == 1 ? "0" + parts[2] : parts[2];
        return year + "-" + month + "-" + day;
    }

    public Map<String, Object> getMonthlyChartSummary(String accountnum) {
        List<TransactionModel> transactions = transactionRepository.findAllByAccountnum(accountnum);

        // Buat list bulan Januari sampai sekarang
        String[] monthNames = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        List<String> labels = new ArrayList<>();
        List<Integer> incomeData = new ArrayList<>();
        List<Integer> expenseData = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue(); // 1-12

        for (int m = 1; m <= currentMonth; m++) {
            final int month = m;
            final int year = now.getYear();

            labels.add(monthNames[month - 1]);

            int totalIncome = transactions.stream()
                    .filter(t -> {
                        try {
                            LocalDateTime dateTime = LocalDateTime.parse(t.getDateTime());
                            return "income".equalsIgnoreCase(t.getType()) &&
                                    dateTime.getMonthValue() == month &&
                                    dateTime.getYear() == year;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .mapToInt(TransactionModel::getAmount)
                    .sum();

            int totalExpense = transactions.stream()
                    .filter(t -> {
                        try {
                            LocalDateTime dateTime = LocalDateTime.parse(t.getDateTime());
                            return "expense".equalsIgnoreCase(t.getType()) &&
                                    dateTime.getMonthValue() == month &&
                                    dateTime.getYear() == year;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .mapToInt(TransactionModel::getAmount)
                    .sum();

            incomeData.add(totalIncome);
            expenseData.add(totalExpense);
        }


        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("datasets", List.of(
                Map.of(
                        "label", "Income",
                        "data", incomeData,
                        "borderColor", "rgb(75, 192, 192)",
                        "backgroundColor", "rgba(75, 192, 192, 0.2)"
                ),
                Map.of(
                        "label", "Expense",
                        "data", expenseData,
                        "borderColor", "rgb(255, 99, 132)",
                        "backgroundColor", "rgba(255, 99, 132, 0.2)"
                )
        ));

        return response;
    }

    public Map<String, Object> getDonutChartSummary(String accountnum, int monthsAgo) {
        Map<String, Object> summary = getTransactionSummaryByMonth(accountnum, monthsAgo);

        int income = (int) summary.get("totalIncome");
        int expense = (int) summary.get("totalExpense");
        int total = income + expense;

        int incomePct = total == 0 ? 0 : (int) Math.round((income * 100.0) / total);
        int expensePct = total == 0 ? 0 : (int) Math.round((expense * 100.0) / total);

        return Map.of(
                "labels", List.of("Income", "Expense"),
                "datasets", List.of(Map.of(
                        "data", List.of(incomePct, expensePct),
                        "backgroundColor", List.of("rgb(75, 192, 192)", "rgb(255, 99, 132)"),
                        "hoverOffset", 4
                )),
                "totalIncome", income,
                "totalExpense", expense
        );
    }



}


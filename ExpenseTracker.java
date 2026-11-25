import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple console Expense Tracker.
 * Features: add, list, delete, total, category summary, CSV persistence.
 * Compile: javac ExpenseTracker.java
 * Run:     java ExpenseTracker
 */
public class ExpenseTracker {

    private static final String FILE_NAME = "expenses.csv";

    // ---------- Model ----------
    static class Expense {
        final int id;
        final String description;
        final String category;
        final double amount;
        final LocalDate date;

        Expense(int id, String description, String category, double amount, LocalDate date) {
            this.id = id;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }

        String toCsv() {
            return id + "," + description.replace(",", ";") + ","
                    + category.replace(",", ";") + "," + amount + "," + date;
        }

        static Expense fromCsv(String line) {
            String[] p = line.split(",");
            return new Expense(Integer.parseInt(p[0]), p[1], p[2],
                    Double.parseDouble(p[3]), LocalDate.parse(p[4]));
        }

        @Override
        public String toString() {
            return String.format("%-4d %-12s %-15s %-20s %10.2f", id, date, category, description, amount);
        }
    }

    // ---------- State ----------
    private final List<Expense> expenses = new ArrayList<>();
    private int nextId = 1;

    // ---------- Operations ----------
    void addExpense(String description, String category, double amount, LocalDate date) {
        expenses.add(new Expense(nextId++, description, category, amount, date));
        save();
        System.out.println("Expense added.");
    }

    void listExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }
        System.out.printf("%-4s %-12s %-15s %-20s %10s%n", "ID", "Date", "Category", "Description", "Amount");
        System.out.println("-".repeat(65));
        expenses.forEach(System.out::println);
    }

    void deleteExpense(int id) {
        boolean removed = expenses.removeIf(e -> e.id == id);
        if (removed) {
            save();
            System.out.println("Expense deleted.");
        } else {
            System.out.println("No expense found with ID " + id);
        }
    }

    void showTotal() {
        double total = expenses.stream().mapToDouble(e -> e.amount).sum();
        System.out.printf("Total spent: %.2f%n", total);
    }

    void showCategorySummary() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }
        Map<String, Double> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.category.toLowerCase(),
                        TreeMap::new, Collectors.summingDouble(e -> e.amount)));
        System.out.println("Spending by category:");
        byCategory.forEach((cat, sum) -> System.out.printf("  %-15s %10.2f%n", cat, sum));
    }

    // ---------- Persistence ----------
    private void save() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Expense e : expenses) out.println(e.toCsv());
        } catch (IOException ex) {
            System.out.println("Could not save data: " + ex.getMessage());
        }
    }

    private void load() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                Expense e = Expense.fromCsv(line);
                expenses.add(e);
                nextId = Math.max(nextId, e.id + 1);
            }
        } catch (IOException | RuntimeException ex) {
            System.out.println("Could not load data: " + ex.getMessage());
        }
    }

    // ---------- UI ----------
    public static void main(String[] args) {
        ExpenseTracker tracker = new ExpenseTracker();
        tracker.load();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Expense Tracker ===");
            System.out.println("1. Add expense");
            System.out.println("2. View all expenses");
            System.out.println("3. Delete expense");
            System.out.println("4. Show total");
            System.out.println("5. Summary by category");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Description: ");
                        String desc = sc.nextLine().trim();
                        System.out.print("Category: ");
                        String cat = sc.nextLine().trim();
                        System.out.print("Amount: ");
                        double amt = Double.parseDouble(sc.nextLine().trim());
                        if (amt <= 0) throw new IllegalArgumentException("Amount must be positive.");
                        System.out.print("Date (yyyy-mm-dd, blank = today): ");
                        String d = sc.nextLine().trim();
                        LocalDate date = d.isEmpty() ? LocalDate.now() : LocalDate.parse(d);
                        tracker.addExpense(desc, cat, amt, date);
                    }
                    case "2" -> tracker.listExpenses();
                    case "3" -> {
                        System.out.print("Expense ID to delete: ");
                        tracker.deleteExpense(Integer.parseInt(sc.nextLine().trim()));
                    }
                    case "4" -> tracker.showTotal();
                    case "5" -> tracker.showCategorySummary();
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception ex) {
                System.out.println("Invalid input: " + ex.getMessage());
            }
        }
    }
}

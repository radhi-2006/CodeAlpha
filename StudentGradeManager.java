import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

class Student {
    private String name;
    private double grade;

    public Student(String name, double grade) {
        this.name = name;
        this.grade = grade;
    }

    public String getName() {
        return name;
    }

    public double getGrade() {
        return grade;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Grade: " + grade;
    }
}

class GradeManager {
    private ArrayList<Student> students;

    public GradeManager() {
        this.students = new ArrayList<>();
    }

    public void addStudent(String name, double grade) {
        students.add(new Student(name, grade));
    }

    public double calculateAverageGrade() {
        if (students.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Student student : students) {
            sum += student.getGrade();
        }
        return sum / students.size();
    }

    public Student getHighestScoringStudent() {
        if (students.isEmpty()) {
            return null;
        }
        return Collections.max(students, Comparator.comparingDouble(Student::getGrade));
    }

    public Student getLowestScoringStudent() {
        if (students.isEmpty()) {
            return null;
        }
        return Collections.min(students, Comparator.comparingDouble(Student::getGrade));
    }

    public void displaySummaryReport() {
        System.out.println("\n--- Student Grade Summary ---");
        if (students.isEmpty()) {
            System.out.println("No student data available.");
            return;
        }

        for (Student student : students) {
            System.out.println(student);
        }

        System.out.println("\nAverage Grade: " + String.format("%.2f", calculateAverageGrade()));

        Student highest = getHighestScoringStudent();
        if (highest != null) {
            System.out.println("Highest Score: " + highest.getName() + " (" + highest.getGrade() + ")");
        }

        Student lowest = getLowestScoringStudent();
        if (lowest != null) {
            System.out.println("Lowest Score: " + lowest.getName() + " (" + lowest.getGrade() + ")");
        }
        System.out.println("-----------------------------\n");
    }
}

public class StudentGradeManager {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GradeManager gradeManager = new GradeManager();

        int choice;
        do {
            System.out.println("Student Grade Management System");
            System.out.println("1. Add Student Grade");
            System.out.println("2. Display Summary Report");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter student name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter student grade: ");
                    double grade = scanner.nextDouble();
                    scanner.nextLine(); // Consume newline
                    gradeManager.addStudent(name, grade);
                    System.out.println("Student added successfully!\n");
                    break;
                case 2:
                    gradeManager.displaySummaryReport();
                    break;
                case 3:
                    System.out.println("Exiting program. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 3);

        scanner.close();
    }
}

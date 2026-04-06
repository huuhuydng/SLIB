package slib.com.example.entity.users;

public enum Role {
    STUDENT,
    TEACHER,
    LIBRARIAN,
    ADMIN;

    public boolean isStaff() {
        return this == LIBRARIAN || this == ADMIN;
    }

    public boolean isPatron() {
        return this == STUDENT || this == TEACHER;
    }
}

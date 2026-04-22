package com.faceattendance.repository.spec;

import com.faceattendance.model.Student;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class StudentSpecifications {

    private StudentSpecifications() {}

    public static Specification<Student> build(
            String query,
            String department,
            Integer year,
            Boolean faceEnrolled,
            Boolean isActive
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                String q = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), q),
                        cb.like(cb.lower(root.get("lastName")), q),
                        cb.like(cb.lower(root.get("email")), q),
                        cb.like(cb.lower(root.get("ienNumber")), q),
                        cb.like(cb.lower(root.get("rollNumber")), q)
                ));
            }

            if (department != null && !department.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("department"), department.trim()));
            }

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            if (faceEnrolled != null) {
                predicates.add(cb.equal(root.get("faceEnrolled"), faceEnrolled));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}


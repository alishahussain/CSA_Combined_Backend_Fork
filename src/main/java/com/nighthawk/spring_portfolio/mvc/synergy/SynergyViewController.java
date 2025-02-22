package com.nighthawk.spring_portfolio.mvc.synergy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.nighthawk.spring_portfolio.mvc.assignments.Assignment;
import com.nighthawk.spring_portfolio.mvc.assignments.AssignmentJpaRepository;
import com.nighthawk.spring_portfolio.mvc.person.Person;
import com.nighthawk.spring_portfolio.mvc.person.PersonDetailsService;
import com.nighthawk.spring_portfolio.mvc.person.PersonJpaRepository;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/mvc/synergy")
public class SynergyViewController {
    @Autowired
    private GradeJpaRepository gradeRepository;
    
    @Autowired
    private GradeRequestJpaRepository gradeRequestRepository;

    @Autowired
    private AssignmentJpaRepository assignmentRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    @GetMapping("/gradebook")
    public String editGrades(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Person user = personRepository.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You must be a logged in user to view this"
            );
        }

        List<Assignment> assignments = assignmentRepository.findAll();

        // students can't edit grades, teachers can edit
        if (user.hasRoleWithName("ROLE_STUDENT")) {
            List<Grade> studentGrades = gradeRepository.findByStudent(user);
        
            Map<Long, Grade> assignmentGrades = new HashMap<>();
            for (Grade grade : studentGrades) {
                assignmentGrades.put(grade.getAssignment().getId(), grade);
            }
        
            model.addAttribute("assignments", assignments);
            model.addAttribute("assignmentGrades", assignmentGrades);
            return "synergy/view_student_grades";
        } else if (user.hasRoleWithName("ROLE_TEACHER") || user.hasRoleWithName("ROLE_ADMIN")) {
            List<Person> students = personRepository.findPeopleWithRole("ROLE_STUDENT");
            List<Grade> gradesList = gradeRepository.findAll();

            Map<Long, Map<Long, Double>> grades = createGradesMap(gradesList, assignments, students);

            model.addAttribute("assignments", assignments);
            model.addAttribute("students", students);
            model.addAttribute("grades", grades);

            return "synergy/edit_grades";
        }

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must a student, teacher, or admin to view grades."
        );
    }

    private Map<Long, Map<Long, Double>> createGradesMap(List<Grade> gradesList, List<Assignment> assignments, List<Person> students) {
        Map<Long, Map<Long, Double>> gradesMap = new HashMap<>();

        // Ok so these are the default vals
        for (Assignment assignment : assignments) {
            gradesMap.put(assignment.getId(), new HashMap<>());
            for (Person student : students) {
                gradesMap.get(assignment.getId()).put(student.getId(), null);
            }
        }

        for (Grade grade : gradesList) {
            if (gradesMap.containsKey(grade.getAssignment().getId())) {
                gradesMap.get(grade.getAssignment().getId()).put(grade.getStudent().getId(), grade.getGrade());
            }
        }

        return gradesMap;
    }

    @GetMapping("/view-grade-requests")
    public String viewRequests(Model model) {
        List<GradeRequest> requests = gradeRequestRepository.findAll();
        model.addAttribute("requests", requests);
        return "synergy/view_grade_requests";
    }

    @GetMapping("/create-grade-request")
    public String createGradeRequest(Model model) {
        List<Assignment> assignments = assignmentRepository.findAll();
        List<Person> students = personRepository.findPeopleWithRole("ROLE_STUDENT");

        model.addAttribute("assignments", assignments);
        model.addAttribute("students", students);

        return "synergy/create_grade_request";
    }
}

package com.resumepilot.jobmatch.controller;

import com.resumepilot.jobmatch.dto.JobSearchRequest;
import com.resumepilot.jobmatch.entity.JobMatch;
import com.resumepilot.jobmatch.service.JobMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchResourceTest {

    @Mock
    private JobMatchService jobMatchService;

    private JobMatchResource controller;
    private JobMatch match;

    @BeforeEach
    void setUp() {
        controller = new JobMatchResource(jobMatchService);
        match = new JobMatch();
        match.setMatchId(7);
        match.setResumeId(1);
        match.setUserId(2);
        match.setJobTitle("Java Developer");
    }

    @Test
    void analyzeUsesPayloadDefaultsAndDelegates() {
        when(jobMatchService.analyzeJobFit(1, 0, "N/A", "", "No resume content")).thenReturn(match);

        ResponseEntity<JobMatch> response = controller.analyze(1, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(match);
        verify(jobMatchService).analyzeJobFit(1, 0, "N/A", "", "No resume content");
    }

    @Test
    void analyzeUsesProvidedPayloadValues() {
        Map<String, Object> payload = Map.of(
                "userId", 2,
                "jobTitle", "Java Developer",
                "jobDescription", "Spring Boot",
                "resumeContent", "Java resume");
        when(jobMatchService.analyzeJobFit(1, 2, "Java Developer", "Spring Boot", "Java resume"))
                .thenReturn(match);

        ResponseEntity<JobMatch> response = controller.analyze(1, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(match);
    }

    @Test
    void getByResumeReturnsMatches() {
        when(jobMatchService.getMatchesByResume(1)).thenReturn(List.of(match));

        ResponseEntity<List<JobMatch>> response = controller.getByResume(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(match);
    }

    @Test
    void getByUserReturnsMatches() {
        when(jobMatchService.getMatchesByUser(2)).thenReturn(List.of(match));

        ResponseEntity<List<JobMatch>> response = controller.getByUser(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(match);
    }

    @Test
    void getByIdReturnsOptionalMatch() {
        when(jobMatchService.getMatchById(7)).thenReturn(Optional.of(match));

        ResponseEntity<Optional<JobMatch>> response = controller.getById(7);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(match);
    }

    @Test
    void bookmarkDelegatesAndReturnsMessage() {
        ResponseEntity<String> response = controller.bookmark(7);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Match bookmarked successfully");
        verify(jobMatchService).bookmarkMatch(7);
    }

    @Test
    void fetchLinkedInUsesRequestFields() {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Java");
        request.setLocation("Remote");
        List<Map<String, Object>> jobs = List.of(Map.of("title", "Java Dev"));
        when(jobMatchService.fetchJobsFromLinkedIn("Java", "Remote")).thenReturn(jobs);

        ResponseEntity<List<Map<String, Object>>> response = controller.fetchLinkedIn(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(jobs);
    }

    @Test
    void fetchNaukriUsesRequestFields() {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Java");
        request.setLocation("Pune");
        List<Map<String, Object>> jobs = List.of(Map.of("title", "Java Dev"));
        when(jobMatchService.fetchJobsFromNaukri("Java", "Pune")).thenReturn(jobs);

        ResponseEntity<List<Map<String, Object>>> response = controller.fetchNaukri(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(jobs);
    }
}

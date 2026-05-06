package com.resumepilot.jobmatch.service;

import com.resumepilot.jobmatch.client.AiServiceClient;
import com.resumepilot.jobmatch.entity.JobMatch;
import com.resumepilot.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceImplTest {

    @Mock
    private JobMatchRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AiServiceClient aiServiceClient;

    @InjectMocks
    private JobMatchServiceImpl service;

    private JobMatch savedMatch;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "apiKey", "rapid-key");
        ReflectionTestUtils.setField(service, "apiHost", "rapid-host");
        ReflectionTestUtils.setField(service, "apiUrl", "http://jobs.example/search");

        savedMatch = new JobMatch();
        savedMatch.setMatchId(7);
        savedMatch.setResumeId(1);
        savedMatch.setUserId(2);
        savedMatch.setJobTitle("Java Developer");
        savedMatch.setMatchScore(86);
    }

    @Test
    void analyzeJobFitStoresAiResult() {
        when(aiServiceClient.getMatchScoreFromAI(any(), eq("SERVICE"), eq("PRO"))).thenReturn(Map.of(
                "matchScore", 86,
                "missingSkills", "Docker",
                "recommendations", "Add Docker project"));
        when(repository.save(any(JobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobMatch result = service.analyzeJobFit(1, 2, "Java Developer", "Spring Boot", "Java resume");

        assertThat(result.getResumeId()).isEqualTo(1);
        assertThat(result.getUserId()).isEqualTo(2);
        assertThat(result.getSource()).isEqualTo("LINKEDIN");
        assertThat(result.getMatchScore()).isEqualTo(86);
        assertThat(result.getMissingSkills()).isEqualTo("Docker");
        assertThat(result.getRecommendations()).isEqualTo("Add Docker project");
        verify(repository).save(any(JobMatch.class));
    }

    @Test
    void analyzeJobFitFallsBackWhenAiFails() {
        when(aiServiceClient.getMatchScoreFromAI(any(), eq("SERVICE"), eq("PRO"))).thenThrow(new RuntimeException("AI down"));
        when(repository.save(any(JobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobMatch result = service.analyzeJobFit(1, 2, "Java", "Backend", "Resume");

        assertThat(result.getMatchScore()).isZero();
        assertThat(result.getMissingSkills()).contains("Could not connect");
    }

    @Test
    void fetchJobsFromLinkedInParsesRapidApiResponse() {
        String body = "{\"data\":[{\"job_title\":\"Java Dev\",\"employer_name\":\"Acme\",\"job_city\":\"Pune\",\"job_apply_link\":\"http://apply\"}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> jobs = service.fetchJobsFromLinkedIn("Java", "Remote");

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0)).containsEntry("title", "Java Dev")
                .containsEntry("company", "Acme")
                .containsEntry("location", "Pune")
                .containsEntry("url", "http://apply");
    }

    @Test
    void fetchJobsReturnsEmptyListOnApiError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("RapidAPI down"));

        assertThat(service.fetchJobsFromLinkedIn("Java", "Remote")).isEmpty();
    }

    @Test
    void repositoryPassThroughMethodsWork() {
        when(repository.findByResumeId(1)).thenReturn(List.of(savedMatch));
        when(repository.findByUserId(2)).thenReturn(List.of(savedMatch));
        when(repository.findByMatchId(7)).thenReturn(Optional.of(savedMatch));
        when(repository.findByMatchScoreGreaterThan(75)).thenReturn(List.of(savedMatch));

        assertThat(service.getMatchesByResume(1)).containsExactly(savedMatch);
        assertThat(service.getMatchesByUser(2)).containsExactly(savedMatch);
        assertThat(service.getMatchById(7)).contains(savedMatch);
        assertThat(service.getTopMatches(1)).containsExactly(savedMatch);
        assertThat(service.getTailoringRecommendations(7, "JD")).contains("AI");
    }

    @Test
    void bookmarkTogglesMatch() {
        savedMatch.setBookmarked(false);
        when(repository.findById(7)).thenReturn(Optional.of(savedMatch));

        service.bookmarkMatch(7);

        assertThat(savedMatch.isBookmarked()).isTrue();
        verify(repository).save(savedMatch);
    }

    @Test
    void deleteDelegatesToRepository() {
        service.deleteMatch(7);

        verify(repository).deleteById(7);
    }
}

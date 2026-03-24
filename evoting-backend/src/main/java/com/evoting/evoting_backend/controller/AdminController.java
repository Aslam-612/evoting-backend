package com.evoting.evoting_backend.controller;

import org.springframework.web.multipart.MultipartFile;
import com.evoting.evoting_backend.model.*;
import com.evoting.evoting_backend.repository.VoterRepository;
import com.evoting.evoting_backend.service.AdminService;
import com.evoting.evoting_backend.service.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final JwtService jwtService;
    private final VoterRepository voterRepository;

    @PostConstruct
    public void init() {
        adminService.createDefaultAdmin();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String result = adminService.login(request.get("username"), request.get("password"));
        if (result.equals("SUCCESS")) {
            String token = jwtService.generateToken(request.get("username"), "ADMIN");
            response.put("status", "SUCCESS");
            response.put("token", token);
            response.put("message", "Admin login successful!");
        } else {
            response.put("status", "ERROR");
            response.put("message", "Invalid username or password.");
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/voters")
    public ResponseEntity<List<Voter>> getVoters(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllVoters());
    }
    @GetMapping("/voters/states")
    public ResponseEntity<List<String>> getStates(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(voterRepository.findDistinctStates());
    }

    @GetMapping("/voters/cities")
    public ResponseEntity<List<String>> getCities(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String state) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        if (state != null && !state.isEmpty())
            return ResponseEntity.ok(voterRepository.findDistinctCitiesByState(state));
        return ResponseEntity.ok(voterRepository.findDistinctCities());
    }
    @GetMapping("/voters/constituencies")
    public ResponseEntity<List<String>> getConstituencies(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String city) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        if (city != null && !city.isEmpty())
            return ResponseEntity.ok(voterRepository.findDistinctConstituenciesByCity(city));
        return ResponseEntity.ok(voterRepository.findDistinctConstituencies());
    }

    @GetMapping("/voters/filter")
    public ResponseEntity<List<Voter>> filterVoters(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String constituency) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        if (state != null && !state.isEmpty() && city != null && !city.isEmpty() && constituency != null && !constituency.isEmpty())
            return ResponseEntity.ok(voterRepository.findByStateAndCityAndConstituency(state, city, constituency));
        if (state != null && !state.isEmpty() && city != null && !city.isEmpty())
            return ResponseEntity.ok(voterRepository.findByStateAndCity(state, city));
        if (state != null && !state.isEmpty())
            return ResponseEntity.ok(voterRepository.findByState(state));
        if (constituency != null && !constituency.isEmpty())
            return ResponseEntity.ok(voterRepository.findByConstituency(constituency));
        return ResponseEntity.ok(voterRepository.findAll());
    }

    @PostMapping("/voters")
    public ResponseEntity<Voter> addVoter(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Voter voter) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addVoter(voter));
    }

    @DeleteMapping("/voters/{id}")
    public ResponseEntity<Map<String, String>> deleteVoter(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        adminService.deleteVoter(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Voter deleted.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/voters/upload-csv")
    public ResponseEntity<Map<String, String>> uploadCsv(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        Map<String, String> response = new HashMap<>();
        try {
            String content = new String(file.getBytes()).trim();
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".csv") && !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                response.put("status", "ERROR");
                response.put("message", "Invalid file format. Please upload a CSV file only (.csv).");
                return ResponseEntity.badRequest().body(response);
            }
            if (content.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "The uploaded file is empty. Please provide a valid CSV file.");
                return ResponseEntity.badRequest().body(response);
            }

            String[] lines = content.split("\n");
            if (lines.length <= 1) {
                response.put("status", "ERROR");
                response.put("message", "No voters found in the file. Please check the CSV format.");
                return ResponseEntity.badRequest().body(response);
            }

            int added = 0;
            int skipped = 0;
            int invalid = 0;
            int underage = 0;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String[] fields = line.split(",");
                if (fields.length < 4) {
                    invalid++;
                    continue;
                }

                String name = fields[0].trim();
                String aadhar = fields[1].trim();
                String mobile = fields[2].trim();
                String dob = fields[3].trim();

                boolean mobileExists = voterRepository.existsByMobile(mobile);
                boolean aadharExists = voterRepository.existsByAadharNumber(aadhar);

                if (mobileExists || aadharExists) {
                    skipped++;
                    continue;
                }

// Age validation - must be 18+
                try {
                    java.time.LocalDate dateOfBirth = java.time.LocalDate.parse(dob);
                    int age = java.time.Period.between(dateOfBirth, java.time.LocalDate.now()).getYears();
                    if (age < 18) {
                        underage++;
                        continue;
                    }
                } catch (Exception e) {
                    invalid++;
                    continue;
                }

                Voter voter = new Voter();
                voter.setName(name);
                voter.setAadharNumber(aadhar);
                voter.setMobile(mobile);
                voter.setDateOfBirth(dob);
                if (fields.length > 4) voter.setState(fields[4].trim());
                if (fields.length > 5) voter.setCity(fields[5].trim());
                if (fields.length > 6) voter.setConstituency(fields[6].trim());
                voter.setHasVoted(false);
                voter.setIsActive(true);
                voterRepository.save(voter);
                added++;
            }

            // Build response message based on results
            if (added == 0 && skipped == 0 && invalid == 0 && underage == 0) {
                response.put("status", "ERROR");
                response.put("message", "No valid voter data found in the file.");

            } else if (added == 0 && skipped > 0 && underage == 0 && invalid == 0) {
                response.put("status", "INFO");
                response.put("message", "No new voters added. All " + skipped + " voter(s) in the file are already registered.");

            } else if (added == 0 && underage > 0 && skipped == 0) {
                response.put("status", "INFO");
                response.put("message", "No voters added. " + underage + " voter(s) were under 18 and skipped.");

            } else if (added > 0 && skipped == 0 && invalid == 0 && underage == 0) {
                response.put("status", "SUCCESS");
                response.put("message", added + " voter(s) added successfully!");

            } else if (added > 0 && skipped > 0 && underage == 0 && invalid == 0) {
                response.put("status", "SUCCESS");
                response.put("message", added + " new voter(s) added. " + skipped + " voter(s) were already registered and skipped.");

            } else if (added > 0 && invalid > 0 && underage == 0) {
                response.put("status", "SUCCESS");
                response.put("message", added + " voter(s) added. " + invalid + " row(s) were skipped due to invalid format.");

            } else if (added > 0 && underage > 0 && skipped == 0 && invalid == 0) {
                response.put("status", "SUCCESS");
                response.put("message", added + " voter(s) added. " + underage + " underage voter(s) were skipped.");

            } else {
                response.put("status", "INFO");
                response.put("message",
                        "Processed file — Added: " + added +
                                ", Already existed: " + skipped +
                                ", Underage: " + underage +
                                ", Invalid rows: " + invalid
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "CSV processing failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/elections")
    public ResponseEntity<List<Election>> getElections(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllElections());
    }

    @PostMapping("/elections")
    public ResponseEntity<Election> addElection(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Election election) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addElection(election));
    }
    @PutMapping("/elections/{id}")
    public ResponseEntity<Election> updateElection(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Election election) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        election.setId(id);
        return ResponseEntity.ok(adminService.addElection(election));
    }

    @PutMapping("/elections/{id}/status")
    public ResponseEntity<Election> updateStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.updateElectionStatus(id, body.get("status")));
    }

    @DeleteMapping("/elections/{id}")
    public ResponseEntity<Map<String, String>> deleteElection(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        adminService.deleteElection(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Election deleted.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<Candidate>> getCandidates(
            @RequestHeader("Authorization") String authHeader) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getAllCandidates());
    }

    @PostMapping("/candidates")
    public ResponseEntity<Candidate> addCandidate(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Candidate candidate) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.addCandidate(candidate));
    }
    @PutMapping("/candidates/{id}")
    public ResponseEntity<Candidate> updateCandidate(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Candidate candidate) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        candidate.setId(id);
        return ResponseEntity.ok(adminService.addCandidate(candidate));
    }

    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<Map<String, String>> deleteCandidate(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        adminService.deleteCandidate(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Candidate deleted.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/results/{electionId}")
    public ResponseEntity<List<Candidate>> getResults(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long electionId) {
        if (!isValidAdmin(authHeader)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adminService.getResults(electionId));
    }

    private boolean isValidAdmin(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            return jwtService.validateToken(token) &&
                    jwtService.extractRole(token).equals("ADMIN");
        } catch (Exception e) {
            return false;
        }
    }
}
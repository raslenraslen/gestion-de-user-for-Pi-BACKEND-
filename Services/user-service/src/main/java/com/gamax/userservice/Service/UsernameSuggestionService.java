package com.gamax.userservice.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamax.userservice.Entity.User;
import com.gamax.userservice.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsernameSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(UsernameSuggestionService.class);

    @Value("${ai.service.api.key}")
    private String apiKey;

    @Value("${ai.service.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public UsernameSuggestionService(UserRepository userRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.userRepository = userRepository;
    }

    public List<String> suggestAvailableUsernames(String existingUsername) {
        logger.info("Préparation de la suggestion de noms pour l'utilisateur existant: '{}'", existingUsername);

        List<String> aiSuggestions = generateSimilarUsernames(existingUsername);

        if (aiSuggestions != null && aiSuggestions.size() == 1 &&
                (aiSuggestions.get(0).startsWith("Erreur") || aiSuggestions.get(0).startsWith("Une erreur") || aiSuggestions.get(0).startsWith("Impossible") || aiSuggestions.get(0).contains("API Google AI") || aiSuggestions.get(0).contains("Réseau"))) {
            logger.error("La génération IA a retourné un message d'erreur: {}", aiSuggestions.get(0));
            return aiSuggestions;
        }

        List<String> availableSuggestions = new ArrayList<>();
        if (aiSuggestions != null) {
            for (String suggestion : aiSuggestions) {
                if (suggestion != null && !suggestion.trim().isEmpty() && suggestion.length() >= 3 && suggestion.length() <= 30) {
                    if (isUsernameAvailable(suggestion)) {
                        availableSuggestions.add(suggestion);
                    }
                } else if (suggestion != null && !suggestion.trim().isEmpty()) {
                    logger.warn("Suggestion de l'IA ignorée (validation échouée): '{}'", suggestion);
                }
            }
        }

        if (availableSuggestions.isEmpty() && (aiSuggestions == null || aiSuggestions.isEmpty())) {
            logger.warn("La génération IA ou le parsing n'a retourné aucune suggestion valide.");
            return new ArrayList<>();
        } else if (availableSuggestions.isEmpty() && aiSuggestions != null && !aiSuggestions.isEmpty()) {
            logger.warn("L'IA a généré {} suggestions, mais aucune n'était disponible.", aiSuggestions.size());
            return new ArrayList<>();
        }

        logger.info("Trouvé {} suggestions disponibles parmi {} suggestions IA générées.", availableSuggestions.size(), aiSuggestions != null ? aiSuggestions.size() : 0);
        return availableSuggestions;
    }

    private List<String> generateSimilarUsernames(String baseUsername) {
        logger.info("Appel de l'API Google AI pour générer des noms similaires à: '{}'", baseUsername);

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contentsArray = objectMapper.createArrayNode();
        ObjectNode contentObject = objectMapper.createObjectNode();
        ArrayNode partsArray = objectMapper.createArrayNode();
        ObjectNode textPart = objectMapper.createObjectNode();

        textPart.put("text", "Generate 5 creative and unique usernames similar to '" + baseUsername + "' for online gaming. The names should be variations, use different suffixes/prefixes, or have a similar style and length. Ensure each suggestion is on a new line and does not contain numbers or special characters at the beginning or end. Just list the names.");
        partsArray.add(textPart);
        contentObject.set("parts", partsArray);
        contentsArray.add(contentObject);
        requestBody.set("contents", contentsArray);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 100);
        requestBody.set("generationConfig", generationConfig);

        String requestBodyJson = requestBody.toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiEndpoint = "/models/gemini-1.5-flash:generateContent";
        String fullApiUrl = apiUrl + apiEndpoint;

        URI uriWithKey = UriComponentsBuilder.fromUriString(fullApiUrl)
                .queryParam("key", apiKey)
                .build()
                .toUri();

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);

        String aiResponseJson;
        try {
            logger.debug("Envoi de la requête à Google AI: POST {} avec corps {}", uriWithKey, requestBodyJson);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    uriWithKey,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            aiResponseJson = responseEntity.getBody();

            if (responseEntity.getStatusCode() != HttpStatus.OK || aiResponseJson == null || aiResponseJson.trim().isEmpty()) {
                logger.error("Réponse non OK ({}) ou vide reçue de l'API Google AI. Réponse brute: {}", responseEntity.getStatusCode().value(), aiResponseJson);
                String errorFromResponse = extractErrorFromGoogleResponse(aiResponseJson);
                if (errorFromResponse != null) {
                    return List.of("Erreur de l'API de génération : " + errorFromResponse);
                }
                return new ArrayList<>();

            }

            logger.info("Réponse API Google AI reçue. Début du parsing.");
            logger.debug("Réponse brute: {}", aiResponseJson);

            List<String> generatedNames = parseGoogleAiResponse(aiResponseJson);

            if (generatedNames.isEmpty()) {
                logger.warn("Aucune suggestion extraite de la réponse Google AI. Réponse brute: {}", aiResponseJson);
                String feedbackReason = extractBlockReasonFromGoogleResponse(aiResponseJson);
                if (feedbackReason != null && !feedbackReason.isEmpty()) {
                    logger.warn("Génération bloquée par l'IA : {}", feedbackReason);
                } else {
                    logger.warn("Aucune suggestion valide trouvée après parsing.");
                }
                return new ArrayList<>();
            }

            return generatedNames;

        } catch (HttpClientErrorException e) {
            logger.error("Erreur client de l'API Google AI (status: {}): {}", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            String errorMessage = "Erreur client API Google AI.";
            try {
                JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
                if (!errorNode.isMissingNode() && errorNode.isObject()) {
                    String googleErrorMessage = errorNode.path("message").asText();
                    if (!googleErrorMessage.isEmpty()) {
                        errorMessage = "Erreur de l'API de génération : " + googleErrorMessage;
                    }
                }
            } catch (Exception parseException) {
                logger.error("Erreur lors du parsing du message d'erreur client de l'API Google AI", parseException);
            }
            return List.of(errorMessage);

        } catch (HttpServerErrorException e) {
            logger.error("Erreur serveur de l'API Google AI (status: {}): {}", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            String statusCodeValue = e.getStatusCode().value() + "";
            return List.of("Erreur serveur API Google AI (Status: " + statusCodeValue + ")");

        } catch (ResourceAccessException e) {
            logger.error("Erreur réseau ou Timeout lors de l'appel à l'API Google AI", e);
            return List.of("Erreur réseau/Timeout API Google AI.");

        } catch (Exception e) {
            logger.error("Erreur générique lors de l'appel à l'API Google AI ou du parsing de la réponse", e);
            return List.of("Erreur interne lors de la génération de noms.");
        }
    }

    private boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            return userOptional.isEmpty();
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de la disponibilité du nom d'utilisateur '{}' en DB.", username, e);
            return false;
        }
    }

    private List<String> parseGoogleAiResponse(String aiResponseJson) {
        List<String> names = new ArrayList<>();
        if (aiResponseJson == null || aiResponseJson.trim().isEmpty()) {
            logger.warn("Réponse JSON de l'API Google AI vide ou nulle lors du parsing.");
            return names;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(aiResponseJson);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isMissingNode() || !candidatesNode.isArray() || candidatesNode.size() == 0) {
                logger.warn("Le chemin 'candidates' est manquant, n'est pas un tableau ou est vide dans la réponse Google AI. Réponse complète: {}", aiResponseJson);
                return names;
            }

            JsonNode firstCandidate = candidatesNode.get(0);
            JsonNode contentNode = firstCandidate.path("content");

            if (contentNode.isMissingNode()) {
                logger.warn("Le chemin 'candidates[0].content' est manquant dans la réponse Google AI. Réponse complète: {}", aiResponseJson);
                return names;
            }

            JsonNode partsNode = contentNode.path("parts");

            if (partsNode.isMissingNode() || !partsNode.isArray() || partsNode.size() == 0) {
                logger.warn("Le chemin 'candidates[0].content.parts' est manquant, n'est pas un tableau ou est vide dans la réponse Google AI. Réponse complète: {}", aiResponseJson);
                return names;
            }

            JsonNode textNode = partsNode.get(0).path("text");

            if (textNode.isMissingNode() || !textNode.isTextual()) {
                logger.warn("Le chemin 'candidates[0].content.parts[0].text' est manquant ou n'est pas du texte dans la réponse Google AI. Réponse complète: {}", aiResponseJson);
                return names;
            }

            String generatedText = textNode.asText();
            logger.info("Texte généré par l'IA (Google AI): {}", generatedText);

            String[] lines = generatedText.split("\\n");

            for (String line : lines) {
                String cleanName = line.trim();
                cleanName = cleanName.replaceAll("^[\\s]*[\\d\\*\\-]+\\.\\s*", "").trim();
                cleanName = cleanName.replaceAll("^[\\s]*[\\*\\-]+\\s*", "").trim();
                cleanName = cleanName.replaceAll("^[\\s]*-+\\s*", "").trim();

                if (!cleanName.isEmpty() && cleanName.length() >= 3 && cleanName.length() <= 30) {
                    names.add(cleanName);
                } else if (!cleanName.isEmpty()) {
                    logger.warn("Suggestion de nom ignorée (critères non remplis): '{}'", cleanName);
                }
            }

        } catch (Exception e) {
            logger.error("Erreur fatale lors du parsing de la réponse JSON de l'API Google AI", e);
        }

        return names;
    }

    private String extractErrorFromGoogleResponse(String errorResponseJson) {
        if (errorResponseJson == null || errorResponseJson.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(errorResponseJson);
            JsonNode errorNode = rootNode.path("error");
            if (!errorNode.isMissingNode() && errorNode.isObject()) {
                JsonNode messageNode = errorNode.path("message");
                if (!messageNode.isMissingNode() && messageNode.isTextual()) {
                    return messageNode.asText();
                }
                JsonNode detailsNode = errorNode.path("details");
                if (detailsNode.isArray() && detailsNode.size() > 0) {
                    JsonNode firstDetail = detailsNode.get(0);
                    if (firstDetail.has("errorMessage")) {
                        return firstDetail.path("errorMessage").asText();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction du message d'erreur de la réponse Google AI.", e);
        }
        return null;
    }

    private String extractBlockReasonFromGoogleResponse(String aiResponseJson) {
        if (aiResponseJson == null || aiResponseJson.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(aiResponseJson);
            JsonNode promptFeedbackNode = rootNode.path("promptFeedback");
            if (!promptFeedbackNode.isMissingNode() && promptFeedbackNode.has("blockReason")) {
                return promptFeedbackNode.path("blockReason").asText("Inconnu");
            }
            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode firstCandidate = candidatesNode.get(0);
                JsonNode finishReasonNode = firstCandidate.path("finishReason");
                if (!finishReasonNode.isMissingNode() && finishReasonNode.isTextual()) {
                    String reason = finishReasonNode.asText();
                    if ("SAFETY".equals(reason) || "RECITATION".equals(reason)) {
                        return "Génération stoppée (" + reason + ")";
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction de la raison de blocage de la réponse Google AI.", e);
        }
        return null;
    }
}
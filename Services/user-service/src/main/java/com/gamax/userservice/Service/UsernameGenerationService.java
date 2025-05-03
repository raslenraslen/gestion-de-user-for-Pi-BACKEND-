// Dans src/main/java/com/gamax/userservice/Service/UsernameGenerationService.java
package com.gamax.userservice.Service; // <-- Assurez-vous que ce package est correct

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Service
public class UsernameGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(UsernameGenerationService.class);

    @Value("${ai.service.api.key}")
    private String apiKey; // Votre clé API Google qui commence par AIza...

    @Value("${ai.service.api.url}")
    private String apiUrl; // Doit être https://generativelanguage.googleapis.com/v1

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public UsernameGenerationService() {
        this.restTemplate = new RestTemplate();
        // Optionnel: configuration de timeout ici si nécessaire
        // SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // requestFactory.setConnectTimeout(5000); // 5 seconds
        // requestFactory.setReadTimeout(15000); // 15 seconds
        // this.restTemplate.setRequestFactory(requestFactory);
    }


    /**
     * Génère des suggestions de noms d'utilisateur en appelant l'API Google AI (Gemini).
     * @param description La description fournie par l'utilisateur.
     * @return Une liste de suggestions de noms d'utilisateur.
     */
    public List<String> generateUsernames(String description) {
        logger.info("Préparation de l'appel de l'API Google AI pour la description: '{}'", description);

        // --- Construction du corps de la requête JSON pour l'API Google AI (Gemini) ---
        ObjectNode requestBody = objectMapper.createObjectNode();

        ArrayNode contentsArray = objectMapper.createArrayNode();
        ObjectNode contentObject = objectMapper.createObjectNode();
        ArrayNode partsArray = objectMapper.createArrayNode();
        ObjectNode textPart = objectMapper.createObjectNode();

        textPart.put("text", "Generate 5 creative and unique usernames for online gaming based on this description: " + description + ". Ensure each suggestion is on a new line and does not contain numbers or special characters at the beginning or end. Just list the names.");
        partsArray.add(textPart);

        contentObject.set("parts", partsArray);
        contentsArray.add(contentObject);

        requestBody.set("contents", contentsArray);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 100);
        requestBody.set("generationConfig", generationConfig);

        String requestBodyJson = requestBody.toString();
        // --- Fin Construction du corps de la requête ---


        // Configurer les en-têtes de la requête
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        // --- Construire l'URL complète avec la clé API comme paramètre ---
        String apiEndpoint = "/models/gemini-1.5-flash:generateContent"; // <-- Modèle choisi ici (à changer si besoin)

        String fullApiUrl = apiUrl + apiEndpoint;

        URI uriWithKey = UriComponentsBuilder.fromUriString(fullApiUrl)
                .queryParam("key", apiKey)
                .build()
                .toUri();
        // --- Fin Construction de l'URL ---

        // --- Déclaration de requestEntity AVANT le bloc try ---
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers); // <-- Déclaration ici

        String aiResponseJson;
        try { // <-- Le bloc try commence ici

            logger.debug("Envoi de la requête à Google AI: POST {} avec corps {}", uriWithKey, requestBodyJson);

            // Exécuter la requête POST synchrone
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    uriWithKey,           // Utilise l'objet URI construit avec la clé
                    HttpMethod.POST,
                    requestEntity,        // <-- Utilisé ici (variable accessible)
                    String.class
            );

            aiResponseJson = responseEntity.getBody();

            if (aiResponseJson == null || aiResponseJson.trim().isEmpty()) {
                logger.warn("Réponse JSON de l'API Google AI vide ou nulle.");
                String errorFromResponse = extractErrorFromGoogleResponse(aiResponseJson);
                if (errorFromResponse != null) {
                    return List.of("Erreur de l'API de génération : " + errorFromResponse);
                }
                return List.of("Réponse vide reçue de l'API de génération.");
            }

            logger.info("Réponse API Google AI reçue. Début du parsing.");
            logger.debug("Réponse brute: {}", aiResponseJson);

            List<String> generatedNames = parseGoogleAiResponse(aiResponseJson);

            if (generatedNames.isEmpty()) {
                logger.warn("Aucune suggestion extraite de la réponse Google AI pour la description: '{}'. Réponse brute: {}", description, aiResponseJson);
                String feedbackReason = extractBlockReasonFromGoogleResponse(aiResponseJson);
                String errorFromResponse = extractErrorFromGoogleResponse(aiResponseJson);
                if (feedbackReason != null && !feedbackReason.isEmpty()) {
                    return List.of("La génération de noms a été bloquée (Raison: " + feedbackReason + ").");
                } else if (errorFromResponse != null && !errorFromResponse.isEmpty()){
                    return List.of("Erreur de l'API de génération : " + errorFromResponse);
                }
                else {
                    generatedNames.add("Aucune suggestion valide trouvée pour votre description (format de réponse inattendu?).");
                }
            }

            return generatedNames;

        } catch (HttpClientErrorException e) {
            logger.error("Erreur client de l'API Google AI (status: {}): {}", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            String errorMessage = "Erreur client lors de l'appel à l'API de génération.";
            try {
                JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
                if (!errorNode.isMissingNode() && errorNode.isObject()) {
                    String googleErrorMessage = errorNode.path("message").asText();
                    if (!googleErrorMessage.isEmpty()) {
                        errorMessage = "Erreur de l'API de génération : " + googleErrorMessage;
                    } else {
                        JsonNode detailsNode = errorNode.path("details");
                        if (detailsNode.isArray() && detailsNode.size() > 0) {
                            JsonNode firstDetail = detailsNode.get(0);
                            if (firstDetail.has("errorMessage")) {
                                errorMessage = "Erreur de l'API de génération (détail): " + firstDetail.path("errorMessage").asText();
                            }
                        }
                        if (errorMessage.equals("Erreur client lors de l'appel à l'API de génération.")) {
                            logger.error("Impossible de trouver un message d'erreur détaillé dans la réponse 4xx de Google. Réponse brute: {}", e.getResponseBodyAsString());
                        }
                    }
                } else {
                    logger.error("Le corps de la réponse 4xx de Google ne contient pas la structure d'erreur attendue. Réponse brute: {}", e.getResponseBodyAsString());
                }

            } catch (Exception parseException) {
                logger.error("Erreur lors du parsing du message d'erreur client de l'API Google AI", parseException);
                errorMessage = "Erreur lors de l'appel à l'API de génération (parsing erreur client).";
            }
            return List.of(errorMessage);


        } catch (HttpServerErrorException e) {
            logger.error("Erreur serveur de l'API Google AI (status: {}): {}", e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            String statusCodeValue = e.getStatusCode().value() + "";
            return List.of("Erreur serveur de l'API de génération (Status: " + statusCodeValue + ")");


        } catch (ResourceAccessException e) {
            logger.error("Erreur réseau ou Timeout lors de l'appel à l'API Google AI", e);
            return List.of("Erreur réseau ou délai dépassé lors de la communication avec l'API de génération.");


        } catch (Exception e) {
            logger.error("Erreur générique lors de l'appel à l'API Google AI ou du parsing de la réponse", e);
            return List.of("Une erreur inattendue est survenue lors de la génération de noms.");
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
                // Ici, la liste names est vide, le caller ajoutera un message d'erreur si nécessaire.
                return names; // Retourne liste vide si pas de candidats valides
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

                if (!cleanName.isEmpty() && cleanName.length() > 2 && cleanName.length() <= 30) {
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
                logger.warn("Structure d'erreur inattendue dans la réponse Google AI: {}", errorResponseJson);
                return "Format d'erreur inattendu de l'API.";
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
                return "Prompt bloqué (" + promptFeedbackNode.path("blockReason").asText("Inconnu") + ")";
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
// Dans src/main/java/com/gamax/userservice/Controller/GenerationController.java
package com.gamax.userservice.Controller;

import com.gamax.userservice.Service.UsernameSuggestionService;
import com.gamax.userservice.payload.UsernameDescriptionRequest;
import com.gamax.userservice.Service.UsernameGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GenerationController {

    private static final Logger logger = LoggerFactory.getLogger(GenerationController.class);

    @Autowired
    private UsernameGenerationService usernameGenerationService;
    @Autowired
    private UsernameSuggestionService usernameSuggestionService;


    @PostMapping(value = "/generate-usernames", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateUsernames(@RequestBody UsernameDescriptionRequest descriptionRequest) {
        logger.info("Requête reçue pour générer des noms d'utilisateur.");

        if (descriptionRequest == null || descriptionRequest.getDescription() == null || descriptionRequest.getDescription().trim().isEmpty()) {
            logger.warn("Requête invalide: description manquante.");

            return ResponseEntity.badRequest().body("La description est requise pour générer des suggestions.");
        }

        String description = descriptionRequest.getDescription().trim();
        logger.info("Description reçue: '{}'", description);

        List<String> suggestions = usernameGenerationService.generateUsernames(description);


        if (suggestions == null || suggestions.isEmpty()) {
            String errorFromService = "Aucune suggestion valide trouvée.";

            if (suggestions != null && suggestions.size() == 1) {
                errorFromService = suggestions.get(0);
                logger.error("Le service de génération a retourné un message d'erreur: {}", errorFromService);
            } else {
                logger.warn("Le service de génération a retourné une liste vide ou nulle.");
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorFromService);
        }



        String responseBody = suggestions.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.joining("\n"));

        if (responseBody.trim().isEmpty()) {

            logger.warn("Les suggestions jointes sont vides après nettoyage.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Aucune suggestion valide trouvée pour votre description.");
        }

        logger.info("Retourne {} suggestions en texte brut.", suggestions.size());

        return ResponseEntity.ok(responseBody);
    }
    @PostMapping("/suggest-similar-username")
    public ResponseEntity<List<String>> suggestSimilarUsername(@RequestBody UsernameDescriptionRequest request) {
        logger.info("Requête reçue pour suggérer des noms similaires à: '{}'", request.getDescription());

        if (request == null || request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            logger.warn("Requête invalide: nom d'utilisateur manquant pour la suggestion.");
            return ResponseEntity.badRequest().body(Collections.singletonList("Le nom d'utilisateur existant est requis pour les suggestions."));
        }

        String existingUsername = request.getDescription().trim();
        logger.info("Nom existant reçu: '{}'", existingUsername);

        // Appeler la méthode sur l'instance injectée du NOUVEAU service
        List<String> availableSuggestions = usernameSuggestionService.suggestAvailableUsernames(existingUsername); // <-- Maintenant usernameSuggestionService ne sera pas null

        if (availableSuggestions != null && availableSuggestions.size() == 1) {
            String potentialErrorMessage = availableSuggestions.get(0);
            if (potentialErrorMessage.startsWith("Erreur") || potentialErrorMessage.startsWith("Une erreur") || potentialErrorMessage.startsWith("Impossible") || potentialErrorMessage.contains("API Google AI") || potentialErrorMessage.contains("Réseau")) {
                logger.error("Le service de suggestion a retourné un message d'erreur: {}", potentialErrorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(availableSuggestions);
            }
        } else if (availableSuggestions == null) {
            logger.error("Le service de suggestion a retourné null.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList("Une erreur interne est survenue."));
        }

        logger.info("Retourne {} suggestions disponibles.", availableSuggestions.size());
        return ResponseEntity.ok(availableSuggestions);
    }
}
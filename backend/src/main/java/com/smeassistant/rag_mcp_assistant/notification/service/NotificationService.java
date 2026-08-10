package com.smeassistant.rag_mcp_assistant.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String escalationRecipient;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${spring.mail.username}") String fromEmail,
                               @Value("${app.notification.escalation-recipient}") String escalationRecipient) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.escalationRecipient = escalationRecipient;
    }

    /**
     * Envoie un email d'escalade quand le RAG ne trouve pas de réponse.
     */
    public void notifyNoAnswer(String question, String userEmail, String sessionId) {
        if (escalationRecipient == null || escalationRecipient.isBlank()) {
            return; // Pas de destinataire configuré, on ne fait rien
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(escalationRecipient);
        message.setSubject("[ESCALADE] Assistant PME - Question sans réponse");
        message.setText("""
                Une question n'a pas trouvé de réponse dans la base de connaissances.

                ---
                Question : %s
                Utilisateur : %s
                Session : %s
                Horodatage : %s

                ---
                Merci d'intervenir pour répondre à cet utilisateur.
                """.formatted(
                question,
                userEmail != null && !userEmail.isBlank() ? userEmail : "Non fourni",
                sessionId != null ? sessionId : "N/A",
                java.time.OffsetDateTime.now().toString()
        ));

        // Best-effort : un echec d'envoi (limite SMTP, indisponibilite...) ne doit
        // JAMAIS faire echouer la reponse de l'assistant a l'utilisateur.
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Echec envoi email d'escalade pour la question \"{}\" : {}", question, e.getMessage());
        }
    }
}
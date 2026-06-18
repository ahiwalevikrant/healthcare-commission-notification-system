package com.healthcare.ner_service.nlp;

import com.healthcare.ner_service.dto.response.ExtractedEntities;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
@Slf4j
public class NerExtractionService {

    private final StanfordCoreNLP pipeline;

    public NerExtractionService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyFineGrained", "false");
        this.pipeline = new StanfordCoreNLP(props);
        log.info("Stanford NLP pipeline initialized");
    }

    public ExtractedEntities extract(String text) {
        log.info("Extracting entities from dispute text");

        ExtractedEntities.ExtractedEntitiesBuilder builder =
                ExtractedEntities.builder();

        // Run Stanford NLP
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);

        String personName = null;
        String organization = null;
        String location = null;
        String date = null;

        for (CoreEntityMention mention : document.entityMentions()) {
            String entityText = mention.text();
            String entityType = mention.entityType();

            switch (entityType) {
                case "PERSON" -> {
                    if (personName == null) personName = entityText;
                }
                case "ORGANIZATION" -> {
                    if (organization == null) organization = entityText;
                }
                case "STATE_OR_PROVINCE", "LOCATION" -> {
                    if (location == null) location = entityText;
                }
                case "DATE" -> {
                    if (date == null) date = entityText;
                }
            }
        }

        builder.agentName(personName);
        builder.carrierName(organization);
        builder.state(location);
        builder.month(date);

        // Custom regex for domain-specific fields
        builder.agentNpn(extractNpn(text));
        builder.policyId(extractPolicyId(text));

        ExtractedEntities result = builder.build();
        log.info("Extracted entities: {}", result);
        return result;
    }

    private String extractNpn(String text) {
        // NPN is typically 7-10 digits
        Pattern npnPattern = Pattern.compile(
                "(?i)(?:NPN|npn)[:\\s#]*([0-9]{7,10})"
        );
        Matcher matcher = npnPattern.matcher(text);
        if (matcher.find()) return matcher.group(1);

        // Also try standalone 7-10 digit numbers
        Pattern digitPattern = Pattern.compile("\\b([0-9]{7,10})\\b");
        Matcher digitMatcher = digitPattern.matcher(text);
        if (digitMatcher.find()) return digitMatcher.group(1);

        return null;
    }

    private String extractPolicyId(String text) {
        // Policy IDs like EP-9921, POL-001, ABC-12345
        Pattern policyPattern = Pattern.compile(
                "\\b([A-Z]{2,4}-[0-9]{3,6})\\b"
        );
        Matcher matcher = policyPattern.matcher(text);
        if (matcher.find()) return matcher.group(1);
        return null;
    }
}

package com.dejavu.backend.config;

import com.dejavu.backend.model.Confession;
import com.dejavu.backend.repository.ConfessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ConfessionRepository confessionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (confessionRepository.count() == 0) {
            System.out.println("Seeding initial confessions...");
            List<String> seeds = List.of(
                "I blamed my brother for breaking the vase so he would get grounded instead of me.",
                "I secretly sabotaged my best friend's relationship because I was jealous of her happiness.",
                "I stole money from my workplace and framed a coworker who was fired.",
                "I accidentally ran over a dog years ago and drove away without looking back.",
                "I forged my father's signature on a loan and he died thinking he owed the money.",
                "I lied about being sick to avoid attending my grandmother's funeral because I was afraid of death.",
                "I read my sister's diary and used her secrets against her during family arguments.",
                "I let my friend take the fall for a prank that got him suspended from school.",
                "I told my spouse I lost my wedding ring, but I actually sold it to pay off a gambling debt.",
                "I ruined my colleague's presentation so I would get the promotion instead of him.",
                "I never delivered the final letter my grandfather wrote to my grandmother.",
                "I abandoned my pet cat in another neighborhood when I moved because it was inconvenient.",
                "I manipulated a lonely classmate into giving me their answers for the final exams.",
                "I caused the car accident that injured my brother, but everyone thinks it was a drunk driver."
            );
            
            for (String text : seeds) {
                Confession c = new Confession();
                c.setText(text);
                confessionRepository.save(c);
            }
            System.out.println("Seeded " + seeds.size() + " confessions.");
        }
    }
}

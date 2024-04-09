package Thisiscool.database.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.NoArgsConstructor;

@Entity(value = "counters", useDiscriminator = false)
@NoArgsConstructor
public class Counter {
    @Id
    public String key;
    public int value;

    public Counter(String key) {
        this.key = key;
    }
}
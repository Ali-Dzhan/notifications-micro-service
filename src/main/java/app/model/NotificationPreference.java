package app.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private String contactInfo;
}

package com.gamax.userservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Client extends User {
    @Column(name = "client_type")
    private String clientType;
    @Column(name = "loyalty_points",columnDefinition = "int default 0")
    private int loyaltyPoints;
    private long credit; // recheck
}

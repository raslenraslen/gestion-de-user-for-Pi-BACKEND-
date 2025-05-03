package com.gamax.userservice.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends User{
    @Column(name = "admin_permission")
    @JsonProperty("permissions")
    private String permissions;
}

package com.yeoboya.lunch.config.security.domain;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ACCESS_IP")
@Data
@EntityListeners(value = { AuditingEntityListener.class})
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessIp implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "IP_ID", unique = true, nullable = false)
    private Long id;

    @Column(name = "IP_ADDRESS", nullable = false)
    private String ipAddress;

    @Column(name = "BLOCK", nullable = false)
    private boolean block;

}

package com.shepherdmoney.interviewproject.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    
    @NonNull
    private Instant date;

    private double balance;

    @NonNull
    @ManyToOne
    @JoinTable(name="credit_card_balance_history ",
               joinColumns={@JoinColumn(name="balance_history_id")},
               inverseJoinColumns={@JoinColumn(name="credit_card_id")})
    private CreditCard card;

    public BalanceHistory(@NonNull Instant date, @NonNull CreditCard card, double balance) {
        this.date = date;
        this.card = card;
        this.balance = balance;
    }
}

package com.shepherdmoney.interviewproject.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @NonNull
    private String issuanceBank;

    @NonNull
    private String number;

    @NonNull
    @ManyToOne
    @JoinTable(name="my_user_cards",
               joinColumns={@JoinColumn(name="cards_id")},
               inverseJoinColumns={@JoinColumn(name="user_id")})
    private User user;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL)
    private List<BalanceHistory> balanceHistory = new LinkedList<BalanceHistory>();
}

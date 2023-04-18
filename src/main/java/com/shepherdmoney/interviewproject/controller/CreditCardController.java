package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.exception.ApiRequestException;
import com.shepherdmoney.interviewproject.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // Check valid request params
        if (payload == null) throw new ApiRequestException("Payload is Null");
        int userId = payload.getUserId();
        String cardIssuanceBank = payload.getCardIssuanceBank();
        String cardNumber = payload.getCardNumber();
        if (cardNumber == null) 
            throw new ApiRequestException("Malformed payload detected, null card number");

        // Find user associated, if cannot, throw new ApiException
        Optional<User> user = userRepository.findById(userId);
        if (!user.isPresent()) throw new ApiRequestException("Cannot Find User");

        // Card Number must be unique, and if card already exists, it belongs to someone else
        List<CreditCard> cardList = creditCardRepository.findByNumber(cardNumber);
        if (!cardList.isEmpty()) throw new ApiRequestException("Card with the card number already exists");

        // Create a card entity, if any of these is null, it will throw an exception
        CreditCard card = CreditCard.of(cardIssuanceBank, cardNumber, user.get());

        // save the card and return the card ID
        return ResponseEntity.status(HttpStatus.OK)
                             .body(creditCardRepository.save(card).getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // Find user, if not throw a new exception
        Optional<User> user = userRepository.findById(userId);
        if (!user.isPresent()) throw new ApiRequestException("Cannot find user associated with the user ID");

        // Build CardView List and return
        List<CreditCardView> cardViews = new LinkedList<CreditCardView>();
        for (CreditCard c : user.get().getCards())
            cardViews.add(CreditCardView.builder()
                                        .issuanceBank(c.getIssuanceBank())
                                        .number(c.getNumber())
                                        .build());

        return ResponseEntity.status(HttpStatus.OK)
                             .body(cardViews);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // Check for valid request params
        if (creditCardNumber == null) throw new ApiRequestException("Malformed request param: Credit Card Number provided cannot be null");

        // If card does not exist, there are no users associated with it
        List<CreditCard> card = creditCardRepository.findByNumber(creditCardNumber);
        if (card.isEmpty()) throw new ApiRequestException("Cannot find user associated with the credit card");

        return ResponseEntity.status(HttpStatus.OK)
                             .body(card.get(0).getUser().getId());
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        // Ruqest param check
        if (payload == null) throw new ApiRequestException("Payload is null");
        if (payload.length == 0) throw new ApiRequestException("Payload is empty");

        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        logger.info("Today");
        logger.info(today.toString());
        for (UpdateBalancePayload info : payload) {
            logger.info("INFO");
            logger.info(info.toString());
            // Check validity of payload
            String cardNumber = info.getCreditCardNumber();
            if (cardNumber == null) throw new ApiRequestException("Malformed payload detected: null card number");

            Double newBalance = info.getCurrentBalance();
            if (newBalance == 0.0) throw new ApiRequestException("Malformed payload detected: Cannot have transaction with 0.0 balance");

            Instant transactionDate = info.getTransactionTime().truncatedTo(ChronoUnit.DAYS);
            if (transactionDate == null) throw new ApiRequestException("Malformed payload detected: null transaction date");
            if (transactionDate.isAfter(today)) throw new ApiRequestException("Cannot have a transaction in the future");

            // Find card, throw exception if doesn't exist
            List<CreditCard> cardList = creditCardRepository.findByNumber(cardNumber);
            if (cardList.isEmpty()) throw new ApiRequestException("Cannot find a valid credit card associated with card number");
            CreditCard card = cardList.get(0);

            List<BalanceHistory> balanceHistories = card.getBalanceHistory();

            // If no history, add from transaction date to today, the balance, and continue to next payload
            if (balanceHistories.isEmpty()) {
                while (transactionDate.isBefore(today)) {
                    balanceHistories.add(0, new BalanceHistory(transactionDate, card, newBalance));
                    transactionDate = transactionDate.plus(1, ChronoUnit.DAYS);
                }
                balanceHistories.add(0, new BalanceHistory(today, card, newBalance));
            } else {
                // if not up to date, insert today's date as well
                if (balanceHistories.get(0).getDate().truncatedTo(ChronoUnit.DAYS).isBefore(today))
                    balanceHistories.add(0, new BalanceHistory(today, card, balanceHistories.get(0).getBalance()));

                // Fill in any gaps, from end to 1
                for (int i = balanceHistories.size(); i-- > 1; ) {
                    Instant nextDate = balanceHistories.get(i-1).getDate().truncatedTo(ChronoUnit.DAYS);
                    Instant tomorrow = balanceHistories.get(i).getDate().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
                    Double balance = balanceHistories.get(i).getBalance();
                    // fill in the gaps until tomorrow is equal to nextDate
                    while(tomorrow.isBefore(nextDate)) {
                        balanceHistories.add(i, new BalanceHistory(tomorrow, card, balance));
                        tomorrow = tomorrow.plus(1, ChronoUnit.DAYS);
                    }
                }

                // Iterate from end (oldest), update balance
                for (int i = balanceHistories.size(); i-- > 0; ) {
                    BalanceHistory balanceHistory = balanceHistories.get(i);
                    Instant date = balanceHistory.getDate().truncatedTo(ChronoUnit.DAYS);
                    // if before, just continue;
                    if (date.isBefore(transactionDate)) continue;
                    // if after or equals, update balance
                    balanceHistory.setBalance(balanceHistory.getBalance() + newBalance);
                }
            }
            // update Balance history and save
            card.setBalanceHistory(balanceHistories);
            creditCardRepository.save(card);
        }
        return ResponseEntity.status(HttpStatus.OK)
                             .body(0);
    }
    
}

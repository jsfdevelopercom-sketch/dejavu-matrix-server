package com.dejavu.backend.service;

import com.dejavu.backend.model.UserAccount;
import com.dejavu.backend.model.CoinTransaction;
import com.dejavu.backend.repository.UserAccountRepository;
import com.dejavu.backend.repository.CoinTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private CoinTransactionRepository transactionRepository;

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Transactional
    public UserAccount registerUser(String username, String language) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPreferredLanguage(language != null && !language.isEmpty() ? language : "English");
        user.setCoins(100);
        user.setTotalScore(0);
        user.setCurrentStreak(0);
        user.setBestStreak(0);
        user.setOnboardingCompleted(false);
        user = userRepository.save(user);

        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(user.getId());
        tx.setType("SIGNUP_BONUS");
        tx.setAmount(100);
        tx.setBalanceAfter(100);
        transactionRepository.save(tx);

        return user;
    }

    public Optional<UserAccount> getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<UserAccount> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional
    public void saveUser(UserAccount user) {
        userRepository.save(user);
    }

    @Transactional
    public void markOnboardingCompleted(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnboardingCompleted(true);
            userRepository.save(user);
        });
    }

    @Transactional
    public void updateLanguage(Long userId, String language) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPreferredLanguage(language);
            userRepository.save(user);
        });
    }

    @Transactional
    public boolean deductCoins(Long userId, int amount, String reason, Long sessionId) {
        UserAccount user = userRepository.findById(userId).orElseThrow();
        if (user.getCoins() >= amount) {
            user.setCoins(user.getCoins() - amount);
            userRepository.save(user);
            
            CoinTransaction tx = new CoinTransaction();
            tx.setUserId(user.getId());
            tx.setType(reason);
            tx.setAmount(-amount);
            tx.setBalanceAfter(user.getCoins());
            tx.setRoomSessionId(sessionId);
            transactionRepository.save(tx);
            return true;
        }
        return false;
    }

    @Transactional
    public void addCoins(Long userId, int amount, String reason, Long sessionId) {
        UserAccount user = userRepository.findById(userId).orElseThrow();
        user.setCoins(user.getCoins() + amount);
        userRepository.save(user);
        
        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(user.getId());
        tx.setType(reason);
        tx.setAmount(amount);
        tx.setBalanceAfter(user.getCoins());
        tx.setRoomSessionId(sessionId);
        transactionRepository.save(tx);
    }
}

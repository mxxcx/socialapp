package com.ninja.socialapp.service;

import com.ninja.socialapp.domain.Proxy;
import com.ninja.socialapp.domain.TwitterAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Service Implementation for managing Twitter API.
 */
@Service
@Transactional
public class TwitterApiService {

    private final Twitter twitterClient;

    private final TwitterAccount twitterAccount;

    private final Proxy proxy;

    private final int currentMonth;

    private final int currentYear;

    private final Logger log = LoggerFactory.getLogger(TwitterApiService.class);

    public TwitterApiService(TwitterAccount twitterAccount, Proxy proxy){
        LocalDate localDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        currentMonth = localDate.getMonthValue();
        currentYear = localDate.getYear();

        this.proxy = proxy;
        this.twitterAccount = twitterAccount;
        this.twitterClient = getTwitterInstance();
    }

    private Twitter getTwitterInstance() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
            .setOAuthConsumerKey(twitterAccount.getConsumerKey())
            .setOAuthConsumerSecret(twitterAccount.getConsumerSecret())
            .setOAuthAccessToken(twitterAccount.getAccessToken())
            .setOAuthAccessTokenSecret(twitterAccount.getAccessTokenSecret());
        cb.setHttpProxyHost(proxy.getHost())
            .setHttpProxyPort(proxy.getPort())
            .setHttpProxyUser(proxy.getUsername())
            .setHttpProxyPassword(proxy.getPassword());

        TwitterFactory twitter = new TwitterFactory(cb.build());
        return twitter.getInstance();
    }

    public User updateProfile() {
        try {
            return twitterClient.updateProfile(twitterAccount.getName(), twitterAccount.getUrl(),
                twitterAccount.getLocation(), twitterAccount.getDescription());
        } catch (TwitterException ex) {
        }
        return null;
    }
}

package com.ninja.socialapp.service;

import com.ninja.socialapp.domain.TwitterAccount;
import com.ninja.socialapp.domain.enumeration.CompetitorStatus;
import com.ninja.socialapp.domain.enumeration.TwitterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TwitterSchedulerService {

    private final Logger log = LoggerFactory.getLogger(TwitterSchedulerService.class);

    private final TwitterAccountService twitterAccountService;

    private final TwitterApiService twitterApiService;

    private final CompetitorService competitorService;

    public TwitterSchedulerService(TwitterAccountService twitterAccountService, TwitterApiService twitterApiService, CompetitorService competitorService) {
        this.twitterAccountService = twitterAccountService;
        this.twitterApiService = twitterApiService;
        this.competitorService = competitorService;
    }

    /**
     * We check for newly added/updated twitter accounts and we modify them via the twitter API
     * <p>
     * This is scheduled to get fired every 2 minutes.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 */2 * * * *" )
    public void updateAccounts() {
        log.debug("Run scheduled update accounts {}");
        List<TwitterAccount> accounts = twitterAccountService.findAllByStatus(TwitterStatus.PENDING_UPDATE);
        for (TwitterAccount account : accounts){
            twitterApiService.updateAccount(account);
        }
    }

    /**
     * We check for competitors and like their followers keeping a cursor
     * <p>
     * This is scheduled to get fired every 1 minute.
     * </p>
     */
    @Async
    @Scheduled(cron = "30 * * * * *")
    public void processCompetitors() {
        log.debug("Run scheduled add followers {}");
        competitorService.findOneByStatusOrderByIdDesc(CompetitorStatus.IN_PROGRESS).ifPresent(competitor -> {
            List<TwitterAccount> accounts = twitterAccountService.findAllByStatus(TwitterStatus.IDLE);
            // we update our statuses
//            competitor.setStatus(CompetitorStatus.IN_PROGRESS);
//            competitorService.save(competitor);
//            for (TwitterAccount account : accounts) {
//                account.setStatus(TwitterStatus.WORKING);
//                twitterAccountService.save(account);
//            }

            // if cursor -1 update to done
            long cursor = competitor.getCursor();
            for (TwitterAccount account : accounts) {
                if (cursor == 0){
                    // we don't want to save multiple times
                    if(competitor.getStatus() != CompetitorStatus.DONE){
                        competitor.setStatus(CompetitorStatus.DONE);
                        competitorService.save(competitor);
                    }
                    account.setStatus(TwitterStatus.IDLE);
                    twitterAccountService.save(account);
                    // no point moving on as competitor followers are done
                    continue;
                }
                cursor = twitterApiService.setupFollowers(account, cursor, competitor.getUserid());
            }

        });

    }
}

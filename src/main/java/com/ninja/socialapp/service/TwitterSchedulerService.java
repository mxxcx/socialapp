package com.ninja.socialapp.service;

import com.ninja.socialapp.domain.Competitor;
import com.ninja.socialapp.domain.TwitterAccount;
import com.ninja.socialapp.domain.TwitterKeyword;
import com.ninja.socialapp.domain.TwitterSettings;
import com.ninja.socialapp.domain.enumeration.CompetitorStatus;
import com.ninja.socialapp.domain.enumeration.KeywordStatus;
import com.ninja.socialapp.domain.enumeration.TwitterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TwitterSchedulerService {

    private final Logger log = LoggerFactory.getLogger(TwitterSchedulerService.class);

    private final TwitterAccountService twitterAccountService;

    private final TwitterApiService twitterApiService;

    private final CompetitorService competitorService;

    private final TwitterErrorService twitterErrorService;

    private final TwitterSettingsService twitterSettingsService;

    private final TwitterKeywordService twitterKeywordService;

    public TwitterSchedulerService(TwitterAccountService twitterAccountService, TwitterApiService twitterApiService,
                                   CompetitorService competitorService, TwitterErrorService twitterErrorService,
                                   TwitterSettingsService twitterSettingsService, TwitterKeywordService twitterKeywordService) {
        this.twitterAccountService = twitterAccountService;
        this.twitterApiService = twitterApiService;
        this.competitorService = competitorService;
        this.twitterErrorService = twitterErrorService;
        this.twitterSettingsService = twitterSettingsService;
        this.twitterKeywordService = twitterKeywordService;
    }

    /**
     * We check for newly added/updated twitter accounts and we modify them via the twitter API
     * <p>
     * This is scheduled to get fired every 1 minutes.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 */1 * * * *")
    public void updateAccounts() {
        log.debug("Run scheduled update accounts {}");
        List<TwitterAccount> twitterAccounts = twitterAccountService.findAllByStatus(TwitterStatus.PENDING_UPDATE);
        for (TwitterAccount twitterAccount : twitterAccounts){
            twitterAccount.setStatus(TwitterStatus.LOCK);
            twitterAccountService.save(twitterAccount);
        }
        twitterApiService.setupUpdateAccounts(twitterAccounts);
    }

    /**
     * We check for competitors and like their followers keeping a cursor
     * <p>
     * This is scheduled to get fired every 4 minute.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 */5 * * * *")
    public void processCompetitors() {
        log.debug("Run scheduled process competitors {}");
        competitorService.findFirstByStatusOrderByIdAsc(CompetitorStatus.IN_PROGRESS).ifPresent((Competitor competitor) -> {
            TwitterSettings twitterSettings = twitterSettingsService.findOne();
            List<TwitterAccount> accounts = twitterAccountService.findAllByStatus(TwitterStatus.IDLE);
            twitterApiService.refreshDate();

            competitor.setStatus(CompetitorStatus.LOCK); // next we update our statuses
            competitorService.save(competitor);
            for (TwitterAccount account : accounts) {
                account.setStatus(TwitterStatus.WORKING);
                twitterAccountService.save(account);
            }

            long cursor = competitor.getCursor() == null ? -1L : competitor.getCursor(); // if cursor -1 update to done
            for (TwitterAccount account : accounts) {
                if (cursor == 0) {
                    if (competitor.getStatus() != CompetitorStatus.DONE) {  // we don't want to save multiple times
                        competitor.setStatus(CompetitorStatus.DONE);
                        competitor.setCreated(Instant.now());
                        competitorService.save(competitor);
                    }
                    account.setStatus(TwitterStatus.IDLE);
                    twitterAccountService.save(account);
                    continue;   // no point moving on as competitor followers are done
                }
                cursor = twitterApiService.setupFollowers(account, cursor, competitor, twitterSettings);
            }

            competitor.setCursor(cursor);  // we save our cursor to keep track and update back our status
            competitor.setStatus(cursor == 0 ? CompetitorStatus.DONE : CompetitorStatus.IN_PROGRESS);
            if (competitor.getStatus() == CompetitorStatus.DONE) competitor.setCreated(Instant.now());
            competitorService.save(competitor);
        });
    }

    /**
     * We delete twitter errors older than 7 days to keep db small. Whe need to delete one by one to delete from search too.
     * <p>
     * This is scheduled to get fired every day.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 0 23 * * *")
    public void deleteTwitterErrors() {
        log.debug("Run scheduled delete old twitter errors {}");
        final int DAYS = 7; // how much time we keep data
        List<Long> olderThanErrorsIds = twitterErrorService.findOlderThan(Instant.now().minus(Duration.ofDays(DAYS)));
        for (Long olderThanErrorsId : olderThanErrorsIds){
            twitterErrorService.delete(olderThanErrorsId);
        }
    }

    /**
     * We check for keywords and add the competitors we find
     * <p>
     * This is scheduled to get fired every 15 minute.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 */15 * * * *")
    public void processKeywords() {
        log.debug("Run scheduled process keywords {}");
        if (competitorService.countAllByStatus(CompetitorStatus.IN_PROGRESS) == 0) {
            twitterKeywordService.findFirstByStatusOrderByIdAsc(KeywordStatus.IN_PROGRESS).ifPresent((TwitterKeyword twitterKeyword) -> {
                TwitterSettings twitterSettings = twitterSettingsService.findOne();
                List<TwitterAccount> accounts = twitterAccountService.findAllByStatus(TwitterStatus.IDLE);
                if (accounts.isEmpty()) return;
                twitterApiService.refreshDate();

                twitterKeyword.setStatus(KeywordStatus.LOCK); // next we update our statuses
                twitterKeywordService.save(twitterKeyword);
                for (TwitterAccount account : accounts) {
                    account.setStatus(TwitterStatus.WORKING);
                    twitterAccountService.save(account);
                }
                final int MAX_PAGE = 51; // as per their documentation
                int page = twitterKeyword.getPage() == null ? 1 : twitterKeyword.getPage(); // if cursor -1 update to done
                for (TwitterAccount account : accounts) {
                    if (page > MAX_PAGE) {
                        if (twitterKeyword.getStatus() != KeywordStatus.DONE) {  // we don't want to save multiple times
                            twitterKeyword.setStatus(KeywordStatus.DONE);
                            twitterKeyword.setCreated(Instant.now());
                            twitterKeywordService.save(twitterKeyword);
                        }
                        account.setStatus(TwitterStatus.IDLE);
                        twitterAccountService.save(account);
                        continue;   // no point moving on as keywords competitors are done
                    }
                    page = twitterApiService.setupCompetitors(account, page, twitterKeyword, twitterSettings);
                }

                twitterKeyword.setPage(page);  // we save our page to keep track and update back our status
                twitterKeyword.setStatus(page == MAX_PAGE ? KeywordStatus.DONE : KeywordStatus.IN_PROGRESS);
                if (twitterKeyword.getStatus() == KeywordStatus.DONE) twitterKeyword.setCreated(Instant.now());
                twitterKeywordService.save(twitterKeyword);
            });
        }
    }

    /**
     * We check for done competitors and we reset older than 3 months done
     * <p>
     * This is scheduled to get fired every week.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 0 0 * * 6") //
    public void resetCompetitors() {
        log.debug("Run scheduled reset twitter competitors {}");
        final int DAYS = 61; // how much time
        List<Competitor> competitors = competitorService.findOlderThanByStatus(
            Instant.now().minus(Duration.ofDays(DAYS)), CompetitorStatus.DONE);
        for (Competitor competitor : competitors) {
            competitorService.reset(competitor);
            competitorService.save(competitor);
        }
    }

    /**
     * We check for done competitors and we reset older than 3 months done
     * <p>
     * This is scheduled to get fired every week.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 0 0 * * 6")
    public void resetKeywords() {
        log.debug("Run scheduled reset twitter keywords {}");
        final int DAYS = 122; // how much time we keep data
        List<TwitterKeyword> keywords = twitterKeywordService.findOlderThanByStatus(
            Instant.now().minus(Duration.ofDays(DAYS)), KeywordStatus.DONE);
        for (TwitterKeyword twitterKeyword : keywords) {
            twitterKeywordService.reset(twitterKeyword);
            twitterKeywordService.save(twitterKeyword);
        }
    }


    /**
     * We check for locked twitter accounts
     * <p>
     * This is scheduled to get fired every 2 day.
     * </p>
     */
    @Async
    @Scheduled(cron = "0 0 0 * * *")
    public void resetLocked() {
        log.debug("Run scheduled reset twitter locked accounts {}");
        List<TwitterAccount> twitterAccounts1 = twitterAccountService.findAllByStatus(TwitterStatus.LOCKED);
        List<TwitterAccount> twitterAccounts2 = twitterAccountService.findAllByStatus(TwitterStatus.AUTH_ERROR);
        List<TwitterAccount> twitterAccounts = new ArrayList<>(twitterAccounts1);
        twitterAccounts.addAll(twitterAccounts2);

        for (TwitterAccount account : twitterAccounts) {
            account.setStatus(TwitterStatus.PENDING_UPDATE);
            twitterAccountService.save(account);
        }
    }
}

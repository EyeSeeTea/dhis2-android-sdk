/*
 * Copyright (c) 2016, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.client.sdk.android.event;

import org.hisp.dhis.client.sdk.android.api.utils.DefaultOnSubscribe;
import org.hisp.dhis.client.sdk.core.common.controllers.SyncStrategy;
import org.hisp.dhis.client.sdk.core.event.EventController;
import org.hisp.dhis.client.sdk.core.event.EventService;
import org.hisp.dhis.client.sdk.models.common.importsummary.ImportSummary;
import org.hisp.dhis.client.sdk.models.common.state.Action;
import org.hisp.dhis.client.sdk.models.common.state.State;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnit;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramStage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;

public class EventInteractorImpl implements EventInteractor {
    public static final String AMERICAN_DATE_FORMAT = "yyyy-MM-dd";
    private final EventService eventService;
    private final EventController eventController;

    public EventInteractorImpl(EventService eventService, EventController eventController) {
        this.eventService = eventService;
        this.eventController = eventController;
    }

    @Override
    public Observable<List<Event>> sync(final Set<String> uids) {
        return sync(SyncStrategy.DEFAULT, uids);
    }

    @Override
    public Observable<List<Event>> sync(final SyncStrategy strategy, final Set<String> uids) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.sync(strategy, uids);
                return eventService.list(uids);
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(Set<String> uids) {
        return pull(SyncStrategy.DEFAULT, uids);
    }

    @Override
    public Observable<List<Event>> pull(final SyncStrategy strategy, final Set<String> uids) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.pull(strategy, uids);
                return eventService.list(uids);
            }
        });
    }

    @Override
    public Observable<Map<String,ImportSummary>> push(final Set<String> uids) {
        return Observable.create(new DefaultOnSubscribe<Map<String,ImportSummary>>() {
            @Override
            public Map<String,ImportSummary> call() {
                Map<String,ImportSummary> importSumariesAndEventsMap = new HashMap<String, ImportSummary>();
                List<ImportSummary> importSummaries = eventController.push(uids);
                for(ImportSummary importSummary:importSummaries){
                    for(String event:uids){
                        if(importSummary.getReference().equals(event)){
                            importSumariesAndEventsMap.put(event,importSummary);
                        }
                    }
                }
                return importSumariesAndEventsMap;
            }
        });
    }

    @Override
    public Event create(OrganisationUnit organisationUnit, Program program,
            ProgramStage programStage, Event.EventStatus status) {
        return eventService.create(organisationUnit, program, programStage, status);
    }

    @Override
    public Observable<Boolean> save(final Event event) {
        return Observable.create(new DefaultOnSubscribe<Boolean>() {
            @Override
            public Boolean call() {
                return eventService.save(event);
            }
        });
    }

    @Override
    public Observable<Boolean> remove(final Event event) {
        return Observable.create(new DefaultOnSubscribe<Boolean>() {
            @Override
            public Boolean call() {
                return eventService.remove(event);
            }
        });
    }

    @Override
    public Observable<Event> get(final long id) {
        return Observable.create(new DefaultOnSubscribe<Event>() {
            @Override
            public Event call() {
                return eventService.get(id);
            }
        });
    }

    @Override
    public Observable<Event> get(final String uid) {
        return Observable.create(new DefaultOnSubscribe<Event>() {
            @Override
            public Event call() {
                return eventService.get(uid);
            }
        });
    }

    @Override
    public Observable<State> get(final Event event) {
        return Observable.create(new DefaultOnSubscribe<State>() {
            @Override
            public State call() {
                return eventService.get(event);
            }
        });
    }

    @Override
    public Observable<Map<Long, State>> map(final List<Event> events) {
        return Observable.create(new DefaultOnSubscribe<Map<Long, State>>() {
            @Override
            public Map<Long, State> call() {
                return eventService.map(events);
            }
        });
    }

    @Override
    public Observable<List<Event>> list() {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                return eventService.list();
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final OrganisationUnit organisationUnit,
            final Program program) {
        return pull(organisationUnit.getUId(), program.getUId());
    }


    @Override
    public Observable<List<Event>> pull(final String organisationUnit,
            final String program) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.pull(organisationUnit, program);
                return eventService.list();
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final String organisationUnit,
            final String program,
            final int maxEvents) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.pull(organisationUnit, program, maxEvents);
                return eventService.list();
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final String organisationUnit,
            final String program, final String startDate, final int maxEvents) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.pull(organisationUnit, program, startDate, maxEvents);
                return eventService.list();
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final String organisationUnit,
            final String program, final String startDate, final String endDate,
            final int maxEvents) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                eventController.pull(organisationUnit, program, startDate, endDate, maxEvents);
                return eventService.list();
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final OrganisationUnit organisationUnit,
            final Program program, final int maxEvents) {
        if (maxEvents > 0) {
            return pull(organisationUnit.getUId(), program.getUId(), maxEvents);
        } else {
            return pull(organisationUnit.getUId(), program.getUId());
        }
    }

    @Override
    public Observable<List<Event>> list(final OrganisationUnit organisationUnit,
            final Program program) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                return eventService.list(organisationUnit, program);
            }
        });
    }

    @Override
    public Observable<List<Event>> pull(final OrganisationUnit organisationUnit,
            final Program program, final Date startDate, final int maxEvents) {
        return pull(organisationUnit.getUId(), program.getUId(), startDate, maxEvents);
    }

    @Override
    public Observable<List<Event>> pull(final String organisationUnit, final String program,
            final Date startDate,
            final int maxEvents) {
        if (startDate == null && maxEvents <= 0) {
            return pull(organisationUnit, program);
        } else {
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(AMERICAN_DATE_FORMAT);
            return pull(organisationUnit, program,
                    DATE_FORMAT.format(startDate),
                    maxEvents);
        }
    }

    @Override
    public Observable<List<Event>> pull(final String organisationUnit, final String program,
            final Date startDate,
            final Date endDate, final int maxEvents) {
        if (endDate == null) {
            return pull(organisationUnit, program,
                    startDate,
                    maxEvents);
        } else if (startDate == null) {
            return pull(organisationUnit, program, maxEvents);
        } else {
            return Observable.create(new DefaultOnSubscribe<List<Event>>() {
                @Override
                public List<Event> call() {
                    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(AMERICAN_DATE_FORMAT);
                    pull(organisationUnit, program,
                            DATE_FORMAT.format(startDate), DATE_FORMAT.format(endDate),
                            maxEvents);
                    return eventService.list();
                }
            });
        }
    }

    @Override
    public Observable<List<Event>> pull(final OrganisationUnit organisationUnit,
            final Program program, final Date startDate, final Date endDate,
            final int maxEvents) {
        return pull(organisationUnit.getUId(), program.getUId(), startDate, endDate, maxEvents);
    }

    @Override
    public Observable<List<Event>> listByActions(final Set<Action> actionSet) {
        return Observable.create(new DefaultOnSubscribe<List<Event>>() {
            @Override
            public List<Event> call() {
                return eventService.listByActions(actionSet);
            }
        });
    }
}

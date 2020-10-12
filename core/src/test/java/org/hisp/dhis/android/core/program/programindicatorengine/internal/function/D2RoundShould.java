package org.hisp.dhis.android.core.program.programindicatorengine.internal.function;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hisp.dhis.android.core.parser.internal.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class D2RoundShould {
    @Mock
    private ExpressionParser.ExprContext context;

    @Mock
    private CommonExpressionVisitor visitor;

    @Mock
    private ExpressionParser.ExprContext mockedFirstExpr;

    private D2Round round = new D2Round();

    @Before
    public void setUp() {
        when(context.expr(0)).thenReturn(mockedFirstExpr);
    }

    @Test
    public void return_argument_rounded_up_to_nearest_whole_number() {
        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("0");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("0"));

        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("0.8");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("1"));

        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("0.4999");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("0"));

        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("0.5001");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("1"));

        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("-9.3");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("-9"));

        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("-9.8");
        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("-10"));
    }

    @Test
    public void return_zero_when_number_is_invalid() {
        when(visitor.castStringVisit(mockedFirstExpr)).thenReturn("not a number");

        MatcherAssert.assertThat(round.evaluate(context, visitor), CoreMatchers.<Object>is("0"));
    }
}

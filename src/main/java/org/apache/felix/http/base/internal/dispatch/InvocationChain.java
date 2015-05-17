/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.dispatch;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.holder.FilterHolder;
import org.apache.felix.http.base.internal.handler.holder.ServletHolder;

public class InvocationChain implements FilterChain
{
    private final ServletHolder servletHolder;
    private final FilterHolder[] filterHolders;

    private int index = -1;

    public InvocationChain(@Nonnull final ServletHolder servletHolder, @Nonnull final FilterHolder[] filterHolders)
    {
        this.filterHolders = filterHolders;
        this.servletHolder = servletHolder;
    }

    @Override
    public final void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException
    {
        if ( this.index == -1 )
        {
            final HttpServletRequest hReq = (HttpServletRequest) req;
            final HttpServletResponse hRes = (HttpServletResponse) res;

            // invoke security
            if ( !servletHolder.getContext().handleSecurity(hReq, hRes))
            {
                // FELIX-3988: If the response is not yet committed and still has the default
                // status, we're going to override this and send an error instead.
                if (!res.isCommitted() && (hRes.getStatus() == SC_OK || hRes.getStatus() == 0))
                {
                    hRes.sendError(SC_FORBIDDEN);
                }

                // we're done
                return;
            }
        }
        this.index++;

        if (this.index < this.filterHolders.length)
        {
            this.filterHolders[this.index].handle(req, res, this);
        }
        else
        {
            // Last entry in the chain...
            this.servletHolder.handle(req, res);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.commandimpl.WaitForDevicesCommand;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.device.Device;
import org.csstudio.scan.device.DeviceContext;
import org.csstudio.scan.logger.DataLogger;
import org.csstudio.scan.logger.MemoryDataLogger;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.ScanInfo;
import org.csstudio.scan.server.ScanState;

/** Scan
 *
 *  <p>Combines a {@link DeviceContext} with {@link ScanContextImpl}ementations
 *  and can execute them.
 *  When a command is executed, it receives a {@link ScanContext} view
 *  of the scan for limited access to the devices, data logger etc.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Scan implements ScanContext
{
    /** Provides the next available <code>id</code> */
    final private static AtomicLong ids = new AtomicLong();

    final private long id;

    final private String name;

    final private Date created = new Date();

    final private DeviceContext devices;

    final private List<ScanCommandImpl<?>> implementations;

    private volatile ScanState state = ScanState.Idle;

    private volatile String error = null;

    private volatile long total_work_units = 0;

    final private MemoryDataLogger data_logger = new MemoryDataLogger();

    final private AtomicLong work_performed = new AtomicLong();

    private volatile ScanCommandImpl<?> current_command = null;

    /** Initialize
     *  @param name User-provided name for this scan
     *  @param devices {@link DeviceContext} to use for scan
     *  @param implementations Commands to execute in this scan
     */
    public Scan(final String name, final DeviceContext devices, ScanCommandImpl<?>... implementations)
    {
        this(name, devices, Arrays.asList(implementations));
    }

    /** Initialize
     *  @param name User-provided name for this scan
     *  @param devices {@link DeviceContext} to use for scan
     *  @param implementations Commands to execute in this scan
     */
    public Scan(final String name, final DeviceContext devices, final List<ScanCommandImpl<?>> implementations)
    {
        id = ids.incrementAndGet();
        this.name = name;
        this.devices = devices;
        this.implementations = implementations;
    }

    /** @return Unique scan identifier (within JVM of the scan engine) */
    public long getId()
    {
        return id;
    }

    /** @return Scan name */
    public String getName()
    {
        return name;
    }

    /** @return Info about current state of this scan */
    public ScanInfo getScanInfo()
    {
        final ScanCommandImpl<?> command = current_command;
        final String command_name;
        if (state == ScanState.Finished)
            command_name = "- end -";
        else if (command != null)
            command_name = command.toString();
        else
            command_name = "";
        return new ScanInfo(id, name, created, state, error, work_performed.get(), total_work_units, command_name);
    }

    /** @return Commands executed by this scan */
    public List<ScanCommand> getScanCommands()
    {
        // Fetch underlying commands for implementations
        final List<ScanCommand> commands = new ArrayList<ScanCommand>(implementations.size());
        for (ScanCommandImpl<?> impl : implementations)
            commands.add(impl.getCommand());
        return commands;
    }

    /** @return Data logger of this scan */
    public DataLogger getDataLogger()
    {
        return data_logger;
    }

    /** {@inheritDoc} */
    @Override
    public Device getDevice(final String name) throws Exception
    {
        return devices.getDeviceByAlias(name);
    }

    /** {@inheritDoc} */
    @Override
    public void logSample(final ScanSample sample)
    {
        data_logger.log(sample);
    }

    /** Execute all commands on the scan,
     *  turning exceptions into a 'Failed' scan state.
     */
    public void execute()
    {
        try
        {
            execute_or_die_trying();
        }
        catch (InterruptedException ex)
        {
            state = ScanState.Aborted;
            error = "Interrupted";
        }
        catch (Throwable ex)
        {
            state = ScanState.Failed;
            error = ex.getMessage();
        }
    }

    /** Execute all commands on the scan,
     *  passing exceptions back up.
     *  @throws Exception on error
     */
    private void execute_or_die_trying() throws Throwable
    {
        // Was scan aborted before it ever got to run?
        if (state == ScanState.Aborted)
            return;
        // Otherwise expect 'Idle'
        if (state != ScanState.Idle)
            throw new IllegalStateException("Cannot run Scan that is " + state);

        // Start Devices
        devices.startDevices();

        // Determine work units
        total_work_units = 1; // WaitForDevicesCommand
        for (ScanCommandImpl<?> command : implementations)
            total_work_units += command.getWorkUnits();

        // Execute commands
        state = ScanState.Running;
        try
        {
            execute(new WaitForDevicesCommand(devices.getDevices()));
            execute(implementations);
            // Successful finish
            state = ScanState.Finished;
        }
        finally
        {
            // Stop devices
            devices.stopDevices();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final List<ScanCommandImpl<?>> commands) throws Exception
    {
        for (ScanCommandImpl<?> command : commands)
        {
            if (state != ScanState.Running  &&
                state != ScanState.Paused)
                return;
            execute(command);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanCommandImpl<?> command) throws Exception
    {
        synchronized (this)
        {
            while (state == ScanState.Paused)
            {
                wait();
            }
        }
        try
        {
            current_command = command;
            command.execute(this);
        }
        catch (InterruptedException ex)
        {
            final String message = "Command aborted: " + command.toString();
            Logger.getLogger(getClass().getName()).log(Level.INFO, message, ex);
            throw ex;
        }
        catch (Exception ex)
        {
            final String message = "Command failed: " + command.toString();
            Logger.getLogger(getClass().getName()).log(Level.WARNING, message, ex);
            throw ex;
        }
    }

    /** Pause execution of a currently executing scan */
    public void pause()
    {
        state = ScanState.Paused;
    }

    /** Resume execution of a paused scan */
    public void resume()
    {
        if (state == ScanState.Paused)
        {
            state = ScanState.Running;
            synchronized (this)
            {
                notifyAll();
            }
        }
    }

    /** Ask for execution to stop */
    void abort()
    {
        state = ScanState.Aborted;
    }

    /** {@inheritDoc} */
    @Override
    public void workPerformed(final int work_units)
    {
        work_performed.addAndGet(work_units);
    }

    // Hash by ID
    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime + (int) (id ^ (id >>> 32));
    }

    // Compare by ID
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof Scan))
            return false;
        final Scan other = (Scan) obj;
        return id == other.getId();
    }
}

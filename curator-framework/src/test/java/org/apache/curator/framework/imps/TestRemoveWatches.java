/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.curator.framework.imps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.BaseClassForTests;
import org.apache.curator.test.Timing;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.WatcherType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRemoveWatches extends BaseClassForTests
{
    @Test
    public void testRemoveCuratorDefaultWatcher() throws Exception
    {
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";            
            client.getCuratorListenable().addListener(new CuratorListener()
            {                
                @Override
                public void eventReceived(CuratorFramework client, CuratorEvent event)
                        throws Exception
                {
                    if(event.getType() == CuratorEventType.WATCHED && event.getWatchedEvent().getType() == EventType.DataWatchRemoved) {                        
                        removedLatch.countDown();
                    }        
                }
            });
                        
            client.checkExists().watched().forPath(path);
            
            client.watches().removeAll().ofType(WatcherType.Data).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveCuratorWatch() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";            
            CuratorWatcher watcher = new CuratorWatcher()
            {
                
                @Override
                public void process(WatchedEvent event) throws Exception
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }
                }
            };
                        
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).ofType(WatcherType.Data).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }    
    
    @Test
    public void testRemoveWatch() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";    
            Watcher watcher = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }                    
                }
            };
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).ofType(WatcherType.Data).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveWatchInBackgroundWithCallback() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {            
            client.start();
         
            //Make sure that the event fires on both the watcher and the callback.
            final CountDownLatch removedLatch = new CountDownLatch(2);
            final String path = "/";
            Watcher watcher = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }                        
                }
            };
            
            BackgroundCallback callback = new BackgroundCallback()
            {
                
                @Override
                public void processResult(CuratorFramework client, CuratorEvent event)
                        throws Exception
                {
                    if(event.getType() == CuratorEventType.REMOVE_WATCHES && event.getPath().equals(path)) {
                        removedLatch.countDown();
                    }
                }
            };
            
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).ofType(WatcherType.Any).inBackground(callback).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveWatchInBackgroundWithNoCallback() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final CountDownLatch removedLatch = new CountDownLatch(1);
            Watcher watcher = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }                    
                }
            };
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).ofType(WatcherType.Any).inBackground().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }        
    
    @Test
    public void testRemoveAllWatches() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final CountDownLatch removedLatch = new CountDownLatch(2);
            
            Watcher watcher1 = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }
                }
            };
            
            Watcher watcher2 = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }                    
                }
            };            
            
            
            client.checkExists().usingWatcher(watcher1).forPath(path);
            client.checkExists().usingWatcher(watcher2).forPath(path);
            
            client.watches().removeAll().ofType(WatcherType.Any).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }  
    
    /**
     * TODO: THIS IS STILL A WORK IN PROGRESS. local() is currently broken if no connection to ZK is available.
     * @throws Exception
     */
    @Test
    public void testRemoveLocalWatch() throws Exception {
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            Watcher watcher = new Watcher()
            {                
                @Override
                public void process(WatchedEvent event)
                {
                    if(event.getPath() == null || event.getType() == null) {
                        return;
                    }
                    
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }
                }
            };            
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            //Stop the server so we can check if we can remove watches locally when offline
            server.stop();
            
            timing.sleepABit();
            
            client.watches().removeAll().ofType(WatcherType.Any).local().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    /**
     * Test the case where we try and remove an unregistered watcher. In this case we expect a NoWatcherException to
     * be thrown. 
     * @throws Exception
     */
    @Test(expectedExceptions=KeeperException.NoWatcherException.class)
    public void testRemoveUnregisteredWatcher() throws Exception
    {
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";            
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent event)
                {
                }                
            };
            
            client.watches().remove(watcher).ofType(WatcherType.Data).forPath(path);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    /**
     * Test the case where we try and remove an unregistered watcher but have the quietly flag set. In this case we expect success. 
     * @throws Exception
     */
    @Test
    public void testRemoveUnregisteredWatcherQuietly() throws Exception
    {
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";            
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent event)
                {
                }                
            };
            
            client.watches().remove(watcher).ofType(WatcherType.Data).quietly().forPath(path);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }    
}
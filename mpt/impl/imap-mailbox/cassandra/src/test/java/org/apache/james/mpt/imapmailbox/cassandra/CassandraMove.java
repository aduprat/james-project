package org.apache.james.mpt.imapmailbox.cassandra;

import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.suite.Move;
import org.junit.After;
import org.junit.Before;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class CassandraMove extends Move {

    private ImapHostSystem system;

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraMailboxTestModule());
        system = injector.getInstance(ImapHostSystem.class);
        super.setUp();
    }
    
    @Override
    protected ImapHostSystem createImapHostSystem() {
        return system;
    }

    @After
    public void tearDown() throws Exception {
        system.afterTest();
    }
    
}

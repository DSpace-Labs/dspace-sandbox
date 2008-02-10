package org.dspace.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ItemManagerTest
{
	private static final String CONFIG = "C:\\workspace\\dspace-working-copy\\config\\dspace.cfg";
    //private static final String CONFIG = "/home/daniele/workspace_DAO/dspace-working-copy/config/dspace.cfg";
    
	private static final String FILEPATH= "C:\\workspace\\chicagoartinstitutelibrary.jpg";
	
    protected static Context context;    
    private static ApplicationService applicationService;
    private static ItemManager itemManager;
    
    public ItemManagerTest() {
        super();
    }
    
    @Before
    public void setUp() {
        try {
        ConfigurationManager.loadConfig(CONFIG);
        context = new Context();
        applicationService = new ApplicationService();  
        itemManager = new ItemManager();
        itemManager.setApplicationService(applicationService);
        } catch (Throwable t) { t.printStackTrace(); }          
    }
    
//    @Test
//    public void testPopulateDBwithBundleBitstream()throws FileNotFoundException, AuthorizeException,  IOException {
//        Item item = applicationService.get(context, Item.class, 1);
//        Bundle bundle;
//        BufferedInputStream is;
//        for(int i=0; i<10; i++) {
//            bundle = itemManager.createBundle(item, context);
//            for(int j=0; j<5; j++) {
//                is= new BufferedInputStream(new FileInputStream(FILEPATH));
//                itemManager.createBitstream(bundle, is, context);
//            }
//        }
//        applicationService.complete();
//    }
    
//    @Test
//    public void testCreateBundle() {
//        Item item = applicationService.get(context, Item.class, 1);
//        Bundle bundle = itemManager.createBundle(item, context);
//        Assert.assertNotNull(bundle);
//        applicationService.abort();
//    }
    
//    @Test
//    public void testCreateBitstream() throws FileNotFoundException, AuthorizeException,  IOException {
//        BufferedInputStream is = new BufferedInputStream(new FileInputStream("FILEPATH"));
//        Bundle bundle = applicationService.get(context, Bundle.class, 1);
//        Bitstream bitstream = itemManager.createBitstream(bundle, is, context);
//        Assert.assertNotNull(bitstream);
//        applicationService.abort();
//    }
    
//    @Test //non funziona!
//    public void testCreateSingleBitstream() throws FileNotFoundException, AuthorizeException,  IOException {
//        BufferedInputStream is = new BufferedInputStream(new FileInputStream(FILEPATH));
//        Item item = applicationService.get(context, Item.class, 1);
//        Bitstream bitstream = itemManager.createSingleBitstream(item, is, context);
//        Assert.assertNotNull(bitstream);
//        applicationService.complete();
//        
//    }
    
//    @Test
//    public void testAddBitstream() {
//        Bitstream bitstream = applicationService.get(context, Bitstream.class, 3);
//        Bundle bundle = applicationService.get(context, Bundle.class, 2);
//        Assert.assertFalse(bitstream.getBundle()==bundle);
//        itemManager.addBitstream(bundle, bitstream);
//        Assert.assertTrue(bitstream.getBundle()==bundle);
//        applicationService.abort();
//    }
    
//    @Test
//    public void testAddBundle() {
//        Bundle bundle = applicationService.get(context, Bundle.class, 1);
//        Item item = applicationService.get(context, Item.class, 4);
//        Assert.assertFalse(bundle.getItem()==item);
//        itemManager.addBundle(item, bundle, context);
//        Assert.assertTrue(bundle.getItem()==item);
//        applicationService.abort();
//    }
    
//    @Test
//    public void testRemoveBitstream() {
//        Bitstream bitstream = applicationService.get(context, Bitstream.class, 9);
//        itemManager.removeBitstream(bitstream.getBundle(), bitstream);
//        Assert.assertTrue(bitstream.isDeleted()==true);
//        applicationService.abort();
//    }
    
//    @Test
//    public void testRemoveBundle() {
//        Bundle bundle = applicationService.get(context, Bundle.class, 10);
//        itemManager.removeBundle(bundle.getItem(), bundle, context);
//        Assert.assertFalse(bundle.getItem().getBundles().contains(bundle));
//        applicationService.abort();
//    }
    
    @Test
    public void testRemoveAllBundles() {
        Item item = applicationService.get(context, Item.class, 3);
        itemManager.removeAllBundles(item, context);
        Assert.assertTrue(item.getBundles()==null);
        applicationService.abort();
    }
}
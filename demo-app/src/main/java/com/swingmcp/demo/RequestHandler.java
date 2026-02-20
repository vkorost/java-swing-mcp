package com.swingmcp.demo;

public class RequestHandler implements Runnable {

    AppLogger log;
    ResourceManager resourceManager = new ResourceManager();
    ComponentManager componentManager = new ComponentManager();
    Order order = null;

    public RequestHandler() {}

    public synchronized void registerOrder(Order o) {
        order = o;
        Thread orderThread = new Thread(this);
        orderThread.start();
    }

    public void run() {
        try {
            synchronized (order) {
                log.info("waiting 5 seconds to validate the order...");
                order.wait(5000);
                log.info("validating the order...");
                order.executeOrder();
            }
            log.info("updating the blotter...");
            BlotterPanel blotter = componentManager.getBlotterPanel();
            blotter.updateOrderStatus(order.getOrderId(), "Executed");
            componentManager.showBlotter();
        } catch (Exception e) {
            log.error("Exception validating the order..." + e, e);
        }
    }
}

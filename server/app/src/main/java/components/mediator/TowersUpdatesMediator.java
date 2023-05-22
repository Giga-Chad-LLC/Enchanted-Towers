package components.mediator;

import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import interactors.CreateTowersResponseInteractor;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TowersUpdatesMediator {
    private final static Logger logger = Logger.getLogger(TowersUpdatesMediator.class.getName());

    private static class Holder {
        static final TowersUpdatesMediator INSTANCE = new TowersUpdatesMediator();
    }

    public static TowersUpdatesMediator getInstance() {
        return Holder.INSTANCE;
    }

    List<StreamObserver<TowersAggregationResponse>> observers = new ArrayList<>();
    CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();

    private TowersUpdatesMediator() {}

    public synchronized void registerObserver(StreamObserver<TowersAggregationResponse> observer) {
        logger.info("Registering new observer " + observer);
        observers.add(observer);
    }

    public synchronized void removeObserver(StreamObserver<TowersAggregationResponse> observer) {
        logger.info("Completing connection and removing observer " + observer);
        observer.onCompleted();
        observers.remove(observer);
    }

    public synchronized void notifyObservers(List<Integer> towerIds) {
        TowersAggregationResponse response = interactor.createResponseWithTowersWithIds(towerIds);
        for (var observer : observers) {
            observer.onNext(response);
        }
    }
}

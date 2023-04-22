package game.logic;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class GameHeap implements Iterable<GameModel>{
    private final PriorityQueue<GameModel> heap;

    public GameHeap() {
        Comparator<GameModel> comparator = (g1, g2) -> {

            // TODO: could change this to allow to compare by game rating median too (in case of draw), or even create
            // a different comparator for each game mode

            if (g1.isFull() && !g2.isFull()) {
                return 1;
            } else if (!g1.isFull() && g2.isFull()) {
                return -1;
            }

            return Integer.compare(g2.getCurrentPlayers(), g1.getCurrentPlayers());
        };
        heap = new PriorityQueue<>(comparator);
    }

    public void addGame(GameModel game) {
        heap.offer(game);
    }

    public GameModel getGameWithMostPlayers() {
        return heap.peek();
    }

    public GameModel removeGameWithMostPlayers() {
        return heap.poll();
    }

    @Override
    public Iterator<GameModel> iterator() {
        return heap.iterator();
    }

    public int getSize() {
        return heap.size();
    }

    public PriorityQueue<GameModel> getHeap() {
        return heap;
    }
}
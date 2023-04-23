package game.logic.structures;

import game.logic.GameModel;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GameHeap implements Iterable<GameModel> {
    private final PriorityQueue<GameModel> heap;
    private final ReadWriteLock lock;

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
        lock = new ReentrantReadWriteLock(true);
    }

    public void addGame(GameModel game) {
        lock.writeLock().lock();
        try {
            heap.offer(game);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public GameModel getGameWithMostPlayers() {
        lock.writeLock().lock();
        try {
            return heap.peek();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public GameModel removeGameWithMostPlayers() {
        lock.writeLock().lock();
        try {
            return heap.poll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Iterator<GameModel> iterator() {
        return heap.iterator();
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return heap.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateHeap(GameModel gameModel) {
        lock.writeLock().lock();
        try {
            heap.remove(gameModel);
            heap.add(gameModel);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public GameModel getGameWithClosestRank(int rank, int rankDelta) {
        lock.writeLock().lock();
        try {
            GameModel closestGame = null;
            int closestRank = Integer.MAX_VALUE;

            for (GameModel game : heap)
                if (game.isAvailable()) {
                    int gameRank = game.getRank();
                    if (Math.abs(gameRank - rank) < closestRank) {
                        closestRank = Math.abs(gameRank - rank);
                        closestGame = game;
                    }
                }

            // only return a game if the rank difference is less than the current rank delta
            if (closestGame != null && closestRank <= rankDelta) return closestGame;
            else return null;

        } finally {
            lock.writeLock().unlock();
        }
    }
}

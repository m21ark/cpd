package game.logic.structures;

import game.server.GameModel;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GameHeap implements Iterable<GameModel>, Serializable {
    private final PriorityQueue<GameModel> heap;
    private final ReadWriteLock lock;

    public GameHeap() {

        heap = new PriorityQueue<>(new GameComparator());
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
        lock.readLock().lock();
        try {
            return heap.iterator();
        } finally {
            lock.readLock().unlock();
        }
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
            GameModel closestGame;
            int closestRank = Integer.MAX_VALUE;

            for (GameModel game : heap)
                if (game.isAvailable()) {
                    int gameRank = game.getRank();
                    if (gameRank == -1 && closestRank == Integer.MAX_VALUE) {
                        return game; // if there's no better option
                    }
                    if (Math.abs(gameRank - rank) < closestRank) {
                        closestRank = Math.abs(gameRank - rank);
                        closestGame = game;

                        if (closestRank <= rankDelta) return closestGame;
                    }

                }

            return null;

        } finally {
            lock.writeLock().unlock();
        }
    }


    private static class GameComparator implements Comparator<GameModel>, Serializable {
        @Override
        public int compare(GameModel g1, GameModel g2) {
            if (g1.isFull() && !g2.isFull()) {
                return 1;
            } else if (!g1.isFull() && g2.isFull()) {
                return -1;
            }

            return Integer.compare(g2.getCurrentPlayers(), g1.getCurrentPlayers());
        }
    }
}

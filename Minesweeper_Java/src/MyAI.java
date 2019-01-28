package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import src.Action.ACTION;

public class MyAI extends AI {

	public static Comparator<TwoTuple> voteComparator = new Comparator<TwoTuple>() {
		@Override
		public int compare(TwoTuple t1, TwoTuple t2) {
			return (int) Math.ceil((t1.votes - t2.votes));
		}
	};

	public static Comparator<TwoTuple> mineComparator = new Comparator<TwoTuple>() {
		@Override
		public int compare(TwoTuple t1, TwoTuple t2) {
			return t2.noOfNeighboringMines - t1.noOfNeighboringMines;
		}
	};

	private class TwoTuple implements Comparable<TwoTuple> {
		public int x;
		public int y;
		public double votes = 0;
		public int noOfNeighboringMines = Integer.MAX_VALUE;
		public boolean flagged = false;
		public boolean visited = false;

		public TwoTuple(int x, int y) {
			this.x = x;
			this.y = y;

		}

		public TwoTuple(int x, int y, int noOfNeighborMines) {
			this.x = x;
			this.y = y;
			this.noOfNeighboringMines = noOfNeighborMines;
		}

		@Override
		public boolean equals(Object e) {
			if (e instanceof TwoTuple) {
				TwoTuple e1 = (TwoTuple) e;
				if (e1.x == this.x && e1.y == this.y)
					return true;
			}

			return false;
		}

		public String toString() {
			return "(" + x + "," + y + ")";
		}

		public ArrayList<TwoTuple> getNeighbors() {

			ArrayList<TwoTuple> neighbors = new ArrayList<>();
			if (isBoardIndexInBounds(this.x - 1, this.y - 1))
				neighbors.add(board[this.x - 1][this.y - 1]);
			if (isBoardIndexInBounds(this.x - 1, this.y))
				neighbors.add(board[this.x - 1][this.y]);
			if (isBoardIndexInBounds(this.x - 1, this.y + 1))
				neighbors.add(board[this.x - 1][this.y + 1]);
			if (isBoardIndexInBounds(this.x, this.y + 1))
				neighbors.add(board[this.x][this.y + 1]);
			if (isBoardIndexInBounds(this.x + 1, this.y + 1))
				neighbors.add(board[this.x + 1][this.y + 1]);
			if (isBoardIndexInBounds(this.x + 1, this.y))
				neighbors.add(board[this.x + 1][this.y]);
			if (isBoardIndexInBounds(this.x + 1, this.y - 1))
				neighbors.add(board[this.x + 1][this.y - 1]);
			if (isBoardIndexInBounds(this.x, this.y - 1))
				neighbors.add(board[this.x][this.y - 1]);

			return neighbors;
		}

		private boolean isBoardIndexInBounds(int x, int y) {
			if (x < rowNum && y >= 1 && x >= 1 && y < colNum)
				return true;
			else
				return false;
		}

		@Override
		public int compareTo(TwoTuple o) {
			if (this.x != o.x)
				return this.x - o.x;
			else
				return this.y - o.y;
		}

	}

	int rowNum = 0;
	int colNum = 0;
	int currX = 0;
	int currY = 0;
	int totalMines = 0;
	TwoTuple[][] board;
	Set<TwoTuple> safeToVisit;
	TwoTuple lastVisited;
	boolean oneEncountered = false;
	int safeToVisitCounter;
	Queue<TwoTuple> minePq = new PriorityQueue<>(2, mineComparator);
	HashSet<TwoTuple> flaggedMines = new HashSet<>();

	private int coveredTiles = 0;
	Matrix m;

	public MyAI(int rowDimension, int colDimension, int totalMines, int startX,
			int startY) {
		rowNum = colDimension + 1;
		colNum = rowDimension + 1;
		this.currX = startX;
		this.currY = startY;
		this.totalMines = totalMines;
		board = new TwoTuple[rowNum][colNum];
		safeToVisit = new TreeSet<TwoTuple>();
		coveredTiles = (rowNum - 1) * (colNum - 1) - 1;

		for (int i = 0; i < rowNum; i++) {
			for (int j = 0; j < colNum; j++) {
				if (i == 0 || j == 0) {
					if (i == 0) {
						board[i][j] = new TwoTuple(i, j, j);
					}

					if (j == 0) {
						board[i][j] = new TwoTuple(i, j, i);
					}

				} else {
					board[i][j] = new TwoTuple(i, j, Integer.MAX_VALUE);
				}
			}
		}
		lastVisited = board[currX][currY];
		lastVisited.visited = true;
		m = new Matrix(board);
	}

	public Action getAction(int number) {

		Queue<TwoTuple> votePq = new PriorityQueue<>(10, voteComparator);
		setNumberOfMines(lastVisited, number);
		lastVisited.visited = true;
		number = lastVisited.noOfNeighboringMines;

//
		if (number < 1) {
			markNeighboursSafe(board[lastVisited.x][lastVisited.y]);
		} else {
			lastVisited.noOfNeighboringMines = number;
			minePq.add(lastVisited);
		}

		if (!safeToVisit.isEmpty()) {
			Iterator<TwoTuple> setIterator = safeToVisit.iterator();
			lastVisited = setIterator.next();
			safeToVisit.remove(lastVisited);
			return uncover(lastVisited);
		} else {
			HashSet<TwoTuple> tilesToFlag = new HashSet<>();
			for (TwoTuple cell : minePq) { // minePq operations
				int countOfCoveredCells = 0;
				ArrayList<TwoTuple> cellsNeighbors = cell.getNeighbors();
				for (TwoTuple celli : cellsNeighbors) {
					if ((celli.visited == false) && (celli.flagged == false)) {
						countOfCoveredCells++;
					}
				}

				if (cell.noOfNeighboringMines == countOfCoveredCells) {
					tilesToFlag.addAll(cell.getNeighbors().stream()
							.filter(icell -> !icell.visited && !icell.flagged)
							.collect(Collectors.toList()));
				}

			}
			flagTiles(tilesToFlag);
		}

		findPatterns();
		if (!safeToVisit.isEmpty()) {
			Iterator<TwoTuple> setIterator = safeToVisit.iterator();
			lastVisited = setIterator.next();
			safeToVisit.remove(lastVisited);
			return uncover(lastVisited);
		}

		m.resetGrid();
		m.prepareGrid();
		m.compute();

		if (!safeToVisit.isEmpty()) {
			Iterator<TwoTuple> setIterator = safeToVisit.iterator();
			lastVisited = setIterator.next();
			safeToVisit.remove(lastVisited);
			return uncover(lastVisited);
		}

		for (int i = 1; i < rowNum; i++) {
			for (int j = 1; j < colNum; j++) {
				TwoTuple cell = board[i][j];
				if (cell.visited == false && cell.flagged == false) {
					List<TwoTuple> nonFlaggedVisitedNeighbors = cell
							.getNeighbors().stream()
							.filter(icell -> icell.visited && !icell.flagged)
							.collect(Collectors.toList());
					if (nonFlaggedVisitedNeighbors.isEmpty())
						continue;
					double sum = 0.0;
					for (TwoTuple twoTuple : nonFlaggedVisitedNeighbors) {
						sum += twoTuple.noOfNeighboringMines;
					}
					cell.votes = sum / nonFlaggedVisitedNeighbors.size();
					votePq.add(cell);
				}
			}
		}

		if (!votePq.isEmpty()) {
			lastVisited = votePq.poll();
			for (TwoTuple twoTuple : votePq) {
				twoTuple.votes = 0.0;
			}
			votePq.clear();
			return uncover(lastVisited);
		}
		return new Action(ACTION.LEAVE);
	}

	private void findPatterns() {

		/**
		 * -1 i 1 -1 * * * 1 2 1 O O O
		 * 
		 * * * * * 1 2 2 1 0 0 0 0
		 * 
		 */

		HashSet<TwoTuple> tilesToFlag = new HashSet();

		// 3 grid patterns
		for (int i = 2; i < rowNum - 1; i++) {
			for (int j = 2; j < colNum - 1; j++) {
				// 121
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& !board[i][j - 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& board[i][j + 1].visited
						&& board[i - 1][j + 1].visited
						&& board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j - 1]);
					tilesToFlag.add(board[i + 1][j - 1]);
				}
				// 121
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i][j - 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& !board[i][j + 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j + 1]);
					tilesToFlag.add(board[i + 1][j + 1]);
				}
				// 121
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i - 1][j].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j + 1].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i + 1][j - 1]);
					tilesToFlag.add(board[i + 1][j + 1]);
				}
				// 121
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& !board[i - 1][j].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j + 1].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j - 1]);
					tilesToFlag.add(board[i - 1][j + 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].visited && !board[i][j - 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& board[i][j + 1].visited
						&& board[i - 1][j + 1].visited
						&& board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i + 1][j - 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].visited && board[i][j - 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& !board[i][j + 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i + 1][j + 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i - 1][j].visited
						&& board[i - 1][j - 1].visited
						&& board[i][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i][j + 1].visited
						&& !board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j + 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i - 1][j].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& board[i - 1][j + 1].visited
						&& board[i][j + 1].visited
						&& board[i + 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j - 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j + 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j + 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j + 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i - 1][j + 1].visited) {
					tilesToFlag.add(board[i + 1][j + 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j + 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i - 1][j + 1].visited) {
					tilesToFlag.add(board[i + 1][j - 1]);
				}
				// 12*
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j + 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j - 1]);
				}
			}
		} 
		/**
		 * visited 1 2 2 1 horizontally unvisited
		 */
		for (int i = 2; i < rowNum - 2; i++) {
			for (int j = 2; j < colNum - 1; j++) {
				// 12 horizontal
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 2
						&& board[i - 1][j].visited && board[i + 2][j].visited
						&& board[i - 1][j + 1].visited
						&& board[i][j + 1].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 2][j + 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 2][j - 1].visited) {
					tilesToFlag.add(board[i + 2][j - 1]);
					markSafe(i - 1, j - 1);
				}
				// 12 horizontal
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 2
						&& board[i - 1][j].visited && board[i + 2][j].visited
						&& board[i - 1][j - 1].visited
						&& board[i][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 2][j - 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i][j + 1].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 2][j + 1].visited) {
					tilesToFlag.add(board[i + 2][j + 1]);
					markSafe(i - 1, j + 1);
				}
//				horizontal 21
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i - 1][j].visited && board[i + 2][j].visited
						&& board[i - 1][j + 1].visited
						&& board[i][j + 1].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 2][j + 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 2][j - 1].visited) {
					tilesToFlag.add(board[i - 1][j - 1]);
					markSafe(i + 2, j - 1);
				}
				// horizontal 21
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i - 1][j].visited && board[i + 2][j].visited
						&& board[i - 1][j - 1].visited
						&& board[i][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 2][j - 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i][j + 1].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 2][j + 1].visited) {
					tilesToFlag.add(board[i - 1][j + 1]);
					markSafe(i + 2, j + 1);
				}
				// horizontal 11
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i - 1][j + 1].visited
						&& board[i][j - 1].visited && board[i][j + 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 2][j].visited
						&& !board[i + 2][j - 1].visited
						&& !board[i + 2][j + 1].visited) {
					markSafe(i + 2, j - 1);
					markSafe(i + 2, j);
					markSafe(i + 2, j + 1);
				}
				// horizontal 11
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 1
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i][j - 1].visited && !board[i][j + 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 2][j].visited
						&& board[i + 2][j - 1].visited
						&& board[i + 2][j + 1].visited) {
					markSafe(i - 1, j - 1);
					markSafe(i - 1, j);
					markSafe(i - 1, j + 1);
				}

				//visited 1 2 2 1 horizontally unvisited
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 2
						&& board[i + 2][j].noOfNeighboringMines == 1
						&& !board[i][j - 1].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 2][j - 1].visited
						&& board[i][j + 1].visited
						&& board[i - 1][j + 1].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 2][j + 1].visited) {
					tilesToFlag.add(board[i][j - 1]);
					tilesToFlag.add(board[i + 1][j - 1]);
					markSafe(i - 1, j - 1);
					markSafe(i + 2, j - 1);
				}

				if (board[i][j].noOfNeighboringMines == 2
						&& board[i - 1][j].noOfNeighboringMines == 1
						&& board[i + 1][j].noOfNeighboringMines == 2
						&& board[i + 2][j].noOfNeighboringMines == 1
						&& board[i][j - 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 2][j - 1].visited
						&& !board[i][j + 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 2][j + 1].visited) {
					tilesToFlag.add(board[i][j + 1]);
					tilesToFlag.add(board[i + 1][j + 1]);
					markSafe(i - 1, j + 1);
					markSafe(i + 2, j + 1);
				}
			}
		}

		for (int i = 2; i < rowNum - 1; i++) {
			for (int j = 2; j < colNum - 2; j++) {
				// 11 vertical
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j - 1].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i - 1][j + 2].visited
						&& !board[i][j + 2].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 1][j + 2].visited) {
					markSafe(i - 1, j + 2);
					markSafe(i, j + 2);
					markSafe(i + 1, j + 2);
				}
				// 11 vertical change j->j+1 and j+2 -> j -1
				if (board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j].noOfNeighboringMines == 1
						&& board[i][j + 2].visited
						&& board[i - 1][j + 2].visited
						&& board[i - 1][j + 1].visited
						&& board[i + 1][j + 2].visited
						&& board[i + 1][j + 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i][j - 1].visited && !board[i + 1][j].visited
						&& !board[i + 1][j - 1].visited) {
					markSafe(i - 1, j - 1);
					markSafe(i, j - 1);
					markSafe(i + 1, j - 1);
				}
				// 12 vertical
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 2
						&& board[i][j - 1].visited && board[i][j + 2].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 1][j + 2].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i - 1][j + 2].visited) {
					tilesToFlag.add(board[i - 1][j + 2]);
					markSafe(i - 1, j - 1);
				}
				// 12 vertical
				if (board[i][j].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 2
						&& board[i][j - 1].visited // && board[i + 2][j].visited
						&& board[i][j + 2].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i - 1][j + 1].visited
						&& board[i - 1][j + 2].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 1][j + 2].visited) {
					tilesToFlag.add(board[i + 1][j + 2]);
					markSafe(i + 1, j - 1);
				}
				// 21 vertical
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j - 1].visited && board[i][j + 2].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 1][j + 2].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i - 1][j + 2].visited) {
					markSafe(i - 1, j + 2);
					tilesToFlag.add(board[i - 1][j - 1]);
				}
				// 21 vertical
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j + 1].noOfNeighboringMines == 1
						&& board[i][j - 1].visited // && board[i + 2][j].visited
						&& board[i][j + 2].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j].visited
						&& board[i - 1][j + 1].visited
						&& board[i - 1][j + 2].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 1][j + 2].visited) {
					tilesToFlag.add(board[i + 1][j - 1]);
					markSafe(i + 1, j + 2);
				}
				// 1221 vertically
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 2
						&& board[i][j + 2].noOfNeighboringMines == 1
						&& !board[i - 1][j].visited
						&& !board[i - 1][j - 1].visited
						&& !board[i - 1][j + 1].visited
						&& !board[i - 1][j + 2].visited
						&& board[i + 1][j].visited
						&& board[i + 1][j - 1].visited
						&& board[i + 1][j + 1].visited
						&& board[i + 1][j + 2].visited) {
					tilesToFlag.add(board[i - 1][j]);
					tilesToFlag.add(board[i - 1][j + 1]);
					markSafe(i - 1, j - 1);
					markSafe(i - 1, j + 2);
				}
				/** 1 2 2 1 vertically **/
				if (board[i][j].noOfNeighboringMines == 2
						&& board[i][j - 1].noOfNeighboringMines == 1
						&& board[i][j + 1].noOfNeighboringMines == 2
						&& board[i][j + 2].noOfNeighboringMines == 1
						&& board[i - 1][j].visited
						&& board[i - 1][j - 1].visited
						&& board[i - 1][j + 1].visited
						&& board[i - 1][j + 2].visited
						&& !board[i + 1][j].visited
						&& !board[i + 1][j - 1].visited
						&& !board[i + 1][j + 1].visited
						&& !board[i + 1][j + 2].visited) {
					tilesToFlag.add(board[i + 1][j]);
					tilesToFlag.add(board[i + 1][j + 1]);
					markSafe(i + 1, j - 1);
					markSafe(i + 1, j + 2);
				}
			}
		}
		flagTiles(tilesToFlag);
	}

	private void flagTiles(HashSet<TwoTuple> tilesToFlag) {
		for (TwoTuple eachTile : tilesToFlag) {
			if (flaggedMines.contains(eachTile))
				continue; // if already flagged ignore
			eachTile.flagged = true;
			eachTile.visited = true;
			totalMines--;
			eachTile.getNeighbors().forEach(n -> setNumberOfMines(n, -1));
			flaggedMines.add(eachTile);
		}

		for (int i = 1; i < rowNum; i++) {
			for (int j = 1; j < colNum; j++) {
				TwoTuple twoTuple = board[i][j];
				if (twoTuple.noOfNeighboringMines == 0) {
					markNeighboursSafe(twoTuple);
				}
			}
		}
	}

	private void flagTile(TwoTuple tileToFlag) {
		if (flaggedMines.contains(tileToFlag))
			return; // if already flagged ignore
		tileToFlag.flagged = true;
		tileToFlag.visited = true;
		tileToFlag.getNeighbors().forEach(n -> setNumberOfMines(n, -1));
		flaggedMines.add(tileToFlag);
		for (int i = 1; i < rowNum; i++) {
			for (int j = 1; j < colNum; j++) {
				TwoTuple twoTuple = board[i][j];
				if (twoTuple.noOfNeighboringMines == 0) {
					markNeighboursSafe(twoTuple);
				}
			}
		}
		totalMines--;
	}

	private Action flag(TwoTuple lastVisited2) {
		flaggedMines.add(lastVisited2);
		return new Action(ACTION.FLAG, lastVisited2.x, lastVisited2.y);
	}

	private Action uncover(TwoTuple lastVisited) {
		coveredTiles--;
		return new Action(ACTION.UNCOVER, lastVisited.x, lastVisited.y);
	}

	private TwoTuple isThereAFreeCorner() {
		TwoTuple[] corner = { board[1][1], board[1][colNum - 1],
				board[rowNum - 1][1], board[rowNum - 1][colNum - 1] };
		for (TwoTuple twoTuple : corner) {
			if (!twoTuple.visited && !twoTuple.flagged) {
				return twoTuple;
			}
		}
		return null;
	}

	private void setNumberOfMines(TwoTuple lastVisited, int number) {
		if (board[lastVisited.x][lastVisited.y].noOfNeighboringMines == Integer.MAX_VALUE) {
			board[lastVisited.x][lastVisited.y].noOfNeighboringMines = number;
		} else {
			board[lastVisited.x][lastVisited.y].noOfNeighboringMines += number;
		}
	}

	private void printboard(TwoTuple[][] board2) {
		for (int i = 0; i < rowNum; i++) {
			for (int j = 0; j < colNum; j++) {
				String no = "";
				int noToPrint = board[i][j].noOfNeighboringMines;
				if (noToPrint == Integer.MAX_VALUE)
					no = "#";
				if (board2[i][j].flagged)
					no = "F";
				else if (board2[i][j].visited)
					no = String.valueOf(noToPrint);
				if (!board2[i][j].visited && !board[i][j].flagged
						&& noToPrint != Integer.MAX_VALUE)
					no = String.valueOf(noToPrint);
				System.out.printf("%-5s", no);
			}
			System.out.println();
		}
	}

	private void markNeighboursSafe(TwoTuple cell) {
		int currX2 = cell.x;
		int currY2 = cell.y;
		markSafe(currX2, currY2);
		markSafe(currX2 - 1, currY2 - 1);
		markSafe(currX2 - 1, currY2);
		markSafe(currX2 - 1, currY2 + 1);
		markSafe(currX2, currY2 + 1);
		markSafe(currX2 + 1, currY2 + 1);
		markSafe(currX2 + 1, currY2);
		markSafe(currX2 + 1, currY2 - 1);
		markSafe(currX2, currY2 - 1);
	}

	private void markSafe(int x, int y) {
		if (x < rowNum && y >= 1 && x >= 1 && y < colNum
				&& !safeToVisit.contains(board[x][y]) && !board[x][y].visited
				&& !board[x][y].flagged)
			safeToVisit.add(board[x][y]);
	}

	private class Matrix {
		double grid[][];
		int r;
		int c;
		final double THRESHOLD = 0.00001;

		Matrix(TwoTuple[][] board) {
			r = (rowNum - 1) * (colNum - 1);
			c = (rowNum - 1) * (colNum - 1) + 1;
			grid = new double[r][c];
		}

		void resetGrid() {
			for (int i = 0; i < r; i++) {
				for (int j = 0; j < c; j++) {
					grid[i][j] = 0;
				}
			}
		}

		void prepareGrid() {
			for (int i = 1; i < rowNum; i++) {
				for (int j = 1; j < colNum; j++) {
					if (board[i][j].visited && !board[i][j].flagged
							&& board[i][j].noOfNeighboringMines != 0) {
						List<TwoTuple> neighbors = board[i][j].getNeighbors()
								.stream().filter(cell -> cell.visited == false)
								.collect(Collectors.toList());

						if (neighbors.isEmpty())
							continue;

						int gridRow = (i - 1) * (colNum - 1) + (j - 1);
						grid[gridRow][c - 1] = board[i][j].noOfNeighboringMines;

						for (TwoTuple n : neighbors) {
							int nIndex = (n.x - 1) * (colNum - 1) + (n.y - 1);
							grid[gridRow][nIndex] = 1;
						}
					}

				}
			}
		}

		private double[][] reduce(double[][] matrix) {
			double[][] echelonMatrix = new double[matrix.length][];
			for (int i = 0; i < matrix.length; i++)
				echelonMatrix[i] = Arrays.copyOf(matrix[i], matrix[i].length);

			int r = 0;
			for (int c = 0; c < echelonMatrix[0].length
					&& r < echelonMatrix.length; c++) {
				int j = r;
				for (int i = r + 1; i < echelonMatrix.length; i++)
					if (Math.abs(echelonMatrix[i][c]) > Math
							.abs(echelonMatrix[j][c]))
						j = i;
				if (Math.abs(echelonMatrix[j][c]) < 0.00001)
					continue;

				double[] temp = echelonMatrix[j];
				echelonMatrix[j] = echelonMatrix[r];
				echelonMatrix[r] = temp;

				double s = 1.0 / echelonMatrix[r][c];
				for (j = 0; j < echelonMatrix[0].length; j++)
					echelonMatrix[r][j] *= s;
				for (int i = 0; i < echelonMatrix.length; i++) {
					if (i != r) {
						double t = echelonMatrix[i][c];
						for (j = 0; j < echelonMatrix[0].length; j++)
							echelonMatrix[i][j] -= t * echelonMatrix[r][j];
					}
				}
				r++;
			}
			return echelonMatrix;
		}

		void compute() {
			double[][] rref;
			rref = reduce(grid);

			for (int i = 0; i < r; i++) {
				ArrayList<TwoTuple> tuples = new ArrayList<>();
				double sum = 0.0;
				double min = 0.0;
				double max = 0.0;
				ArrayList<TwoTuple> negative = new ArrayList<>();
				ArrayList<TwoTuple> positive = new ArrayList<>();
				for (int j = 0; j < c - 1; j++) {
					if (compare(rref[i][j], 0) != 0) {
						int bx = (j) / (colNum - 1) + 1;
						int by = j % (colNum - 1) + 1;
						tuples.add(board[bx][by]);
						sum += rref[i][j];
					}
					if (rref[i][j] < 0) {
						min += rref[i][j];
						int bx = (j) / (colNum - 1) + 1;
						int by = j % (colNum - 1) + 1;
						negative.add(board[bx][by]);
					}
					if (rref[i][j] > 0) {
						max += rref[i][j];
						int bx = (j) / (colNum - 1) + 1;
						int by = j % (colNum - 1) + 1;
						positive.add(board[bx][by]);
					}
				}
				if (tuples.size() == 1) {
					if (compare(rref[i][c - 1], 1) == 0) {
						flagTile(tuples.get(0));
					} else if (compare(rref[i][c - 1], 0) == 0) {
						markSafe(tuples.get(0).x, tuples.get(0).y);
					}
				}
				if (tuples.size() != 0) {
					if (compare(rref[i][c - 1], min) == 0) {
						negative.forEach(t -> flagTile(t));
						positive.forEach(t -> markSafe(t.x, t.y));
					}
					if (compare(rref[i][c - 1], max) == 0) {
						positive.forEach(t -> flagTile(t));
						negative.forEach(t -> markSafe(t.x, t.y));
					}
				}
			}
		}

		private int compare(double one, double two) {
			if (Math.abs(one - two) < THRESHOLD) {
				return 0;
			}
			return 1;
		}

		private void printGrid(double[][] grid2) {
			for (int i = -1; i < r; i++) {
				for (int j = -1; j < c; j++) {
					if (i == -1 || j == -1) {
						if (i == -1 && j == -1) {
							System.out.printf("%d (%d,%d)%-5s", j, 0, 0, "");
							continue;
						}
						if (i == -1) {
							System.out.printf("%d(%d,%d)%-5s", j,
									(j - 1) / (colNum - 1), (j) % (colNum - 1),
									"");
						} else {
							System.out.printf("%d (%d,%d)%-10s", i,
									(i - 1) / (colNum - 1), (i) % (colNum - 1),
									"");
						}

					} else {
						System.out.printf("%-10.1f", grid2[i][j]);
					}
				}
				System.out.println();
			}

		}
	}
}
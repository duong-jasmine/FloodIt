import java.util.ArrayList;
import java.util.Arrays;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*; 
import java.awt.Color;
import java.util.Iterator;
import java.util.Random;

/*
 * GAME RULES:
 * WIN when all cells are the same color and the moves are less than or equal to the total moves
 * LOSE when moves played = total moves and the board is not completely flooded.
 * RESET board with key press "r"
 * game stops when lost
 */

//to represent either a Cell with data or a Cell without data
interface ICells { 

  //is this cell's color equal to the given color?
  boolean sameColor(Color newColor);

  //EFFECT: change the color of this cell to the given color
  void changeColor(Color newColor);

  //EFFECT: add the cell to the given list if flooded or equal to the given color
  void addToWorklist(Color newColor, ArrayList<Cell> alreadyChanged);

  //reset the processed boolean to false
  void resetFlag();
}

//A cell with no data and is not a part of the game board 
class LeafCell implements ICells {
  public boolean isFlooded() {
    return false;
  }

  //is this cell the same color as the given color?
  public boolean sameColor(Color newColor) {
    return false;
  }

  //EFFECT: change the color of this cell to the given color
  public void changeColor(Color newColor) {
    //EFFECT: no effect on LeafCell

  }

  //EFFECT: add the cell to the given list if flooded or equal to the given color
  public void addToWorklist(Color newColor, ArrayList<Cell> alreadyChanged) {
    //EFFECT: no effect on LeafCell

  }

  //reset the processed boolean to false
  public void resetFlag() {
    //EFFECT: no effect on LeafCell

  }
}


//Represents a single square of the game area
class Cell implements ICells {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  Color color;
  boolean flooded;
  // the four adjacent cells to this one
  ICells left;
  ICells top;
  ICells right;
  ICells bottom;
  boolean processed;

  // a constructor that takes in a cell not linked
  Cell(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.flooded = false;
    // the four adjacent cells to this one
    this.left = new LeafCell();
    this.top = new LeafCell();
    this.right = new LeafCell();
    this.bottom = new LeafCell();
  }

  // a constructor that is for a cell with linked 
  Cell(int x, int y, Color color, boolean flooded, ICells left, 
      ICells top, ICells right, ICells bottom) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.flooded = flooded;
    // the four adjacent cells to this one
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  // draws the cell, given scaleFactor. Moves pinhole to the top left corner
  public WorldImage drawCell(int scaleFactor) {
    return new RectangleImage(scaleFactor, scaleFactor, OutlineMode.SOLID, this.color)
        .movePinhole(- scaleFactor / 2, - scaleFactor / 2);

  }

  // is this cell the same color as the given color?
  public boolean sameColor(Color newColor) {
    return this.color.getRGB() == newColor.getRGB();
  }

  // EFFECT: change the color of this cell to the given color
  public void changeColor(Color newColor) {
    this.color = newColor;
  }

  // EFFECT: adds the neighbors to the given list if they meet the conditions of 
  //the addToWorklist helper
  public void process(Color newColor, ArrayList<Cell> worklist) {
    this.left.addToWorklist(newColor, worklist);
    this.right.addToWorklist(newColor, worklist);
    this.top.addToWorklist(newColor, worklist);
    this.bottom.addToWorklist(newColor, worklist);
  }

  //EFFECT: adds this cell to the given list if it is unprocessed and either 
  //flooded or the same color 
  public void addToWorklist(Color newColor, ArrayList<Cell> worklist) {
    if (this.flooded && !this.processed) {
      worklist.add(this);
    }
    else if (!this.processed && this.sameColor(newColor)) {
      this.flooded = true;
      worklist.add(this);
    }
    this.processed = true;
  }

  //returns the processed flag to false
  public void resetFlag() {
    this.processed = false;
  }




}

// a class representing the FloodItWorld current World/scene/state
class FloodItWorld extends World {

  Random rand = new Random();
  ArrayList<ArrayList<Cell>> board;
  int size;
  int numberOfColors;
  int scaleFactor = 20;
  WorldScene gameScene; 

  int moves = 0;
  int totalMoves;

  //list of flooded cells that still need to change colors and add neighbors
  ArrayList<Cell> worklist = new ArrayList<Cell>();
  Color clickedColor;

  //constructor allowing customization for size and number of colors
  FloodItWorld(int size, int numberOfColors) {
    this.numberOfColors = numberOfColors;
    this.size = size;
    this.gameScene = new WorldScene(scaleFactor * size, scaleFactor * (size + 2));
    this.makeBoard();
    this.totalMoves = size * numberOfColors / 3;

  }

  //constructor for testing where a rand is passed in
  FloodItWorld(int size, int numberOfColors, Random rand) {
    this.numberOfColors = numberOfColors;
    this.size = size;
    this.rand = rand;
    this.gameScene = new WorldScene(scaleFactor * size, scaleFactor * (size + 2));
    this.makeBoard();
    this.totalMoves = size * numberOfColors / 3;

  }

  //possible colors that the game can be configured with
  ArrayList<Color> possibleColors = new ArrayList<Color>(Arrays.asList(
      Color.red, Color.blue, Color.pink, Color.black,
      Color.orange, Color.yellow, Color.gray, Color.green));

  // random int generated from 0 to numberOfColors
  Color getNewRandomColor() {
    int randInt = this.rand.nextInt(this.numberOfColors);
    return this.possibleColors.get(randInt); 

  } 

  //creates new board with new cells
  void makeBoard() {

    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>(); 

    //for each row in the array
    for (int i = 0; i < this.size; i++) {

      //add a new list to the end of the CellArray
      board.add(new ArrayList<Cell>());

      //for each column in the array
      for (int j = 0; j < this.size; j++) {

        //add to list i in the array @ end 
        board.get(i).add(new Cell(i * scaleFactor, j * scaleFactor, this.getNewRandomColor()));

        //flood first cell?
        if (j == 0 && i == 0) {
          board.get(i).get(j).flooded = true; 
        }
      }
    }

    linkSides(board, this.size);

    Cell firstCell = board.get(0).get(0);
    this.clickedColor = firstCell.color;
    this.board = board;
  }

  //draws this World 
  public WorldScene makeScene() {
    Iterator<ArrayList<Cell>> iterator = this.board.iterator();

    while (iterator.hasNext()) {
      ArrayList<Cell> currentRow = iterator.next();
      Iterator<Cell> cellIterator = currentRow.iterator();

      while (cellIterator.hasNext()) {

        Cell currentCell = cellIterator.next();

        this.gameScene.placeImageXY(currentCell.drawCell(this.scaleFactor), 
            currentCell.x, currentCell.y);
      }
    }

    WorldImage whiteBack = new RectangleImage(this.scaleFactor * size, this.scaleFactor * 2,
        OutlineMode.SOLID, Color.WHITE)
        .movePinhole(-this.scaleFactor * size / 2, - this.scaleFactor);

    WorldImage score = new TextImage("moves: " + String.valueOf(
        this.moves) + "/" + String.valueOf(this.totalMoves), size * 2, Color.BLACK);

    this.gameScene.placeImageXY(whiteBack, 0, this.size * scaleFactor);
    this.gameScene.placeImageXY(score, this.size * scaleFactor / 2,
        (this.size + 1) * scaleFactor);


    return this.gameScene;
  }

  //EFFECT: links the surrounding cells to each other to create a linked board of cells
  void linkSides(ArrayList<ArrayList<Cell>> currentICells, int size) {
    for (int i = 0; i < currentICells.size(); i++) {
      for (int j = 0; j < currentICells.size(); j++) {

        if (j + 1 < size) { 
          currentICells.get(i).get(j).right = currentICells.get(i).get(j + 1);
        }

        if (j - 1 >= 0 ) {
          currentICells.get(i).get(j).left = currentICells.get(i).get(j - 1);
        }

        if (i - 1 >= 0) {
          currentICells.get(i).get(j).top = currentICells.get(i - 1).get(j);
        }

        if (i + 1 < size) {
          currentICells.get(i).get(j).bottom = currentICells.get(i + 1).get(j);
        }
      }
    }
  }

  //EFFECT: if click is within bounds of the game board, and on a different color
  // sets the clickedColor as the clicked color, increases move count,
  //resets the processed boolean on each cell, and 
  public void onMouseClicked(Posn pos) {
    int posnX = pos.x; 
    int posnY = pos.y; 
    if (pos.x < this.size * this.scaleFactor && posnY < this.size * this.scaleFactor) {

      Cell clicked = this.board.get(posnX / this.scaleFactor).get(posnY /  this.scaleFactor);
      //updates the clicked color
      this.clickedColor = clicked.color;
      Cell firstCell = this.board.get(0).get(0);
      if (!firstCell.sameColor(this.clickedColor)) {
        this.moves++;
        this.resetProcessed();
        this.worklist = new ArrayList<Cell>(Arrays.asList(firstCell));
      }
    }
  }

  @Override
  // Process worklist on tick:
  // - change the color of each flooded cell and add their neighbors to the worklist
  // if applicable
  public void onTick() {
    int wlistSize = this.worklist.size(); 
    //for the size of the worklist 
    //System.out.println(wlistSize);
    for (int i = 0; i < wlistSize; i++) {
      //change the color of the cell
      Cell current = this.worklist.get(0);
      current.changeColor(this.clickedColor); 
      //potentially add neighbors to the flood
      current.process(clickedColor, this.worklist);
      worklist.remove(0);
    }
  }

  // are all of the cells in this board the same color?
  boolean allSameColor() {
    Iterator<ArrayList<Cell>> it = this.board.iterator();
    while (it.hasNext()) {
      ArrayList<Cell> currentRow = it.next();
      Iterator<Cell> currentIt = currentRow.iterator();
      while (currentIt.hasNext()) {
        Cell currentCell = currentIt.next();
        if (!currentCell.sameColor(this.clickedColor)) {
          return false;
        }
      }
    }
    return true;
  }

  //resets the board when r is pressed
  public void onKeyEvent(String key) {
    if ("r".contains(key)) {
      this.moves = 0;

      this.makeBoard();
    }
  }

  //the world ends if either 1. moves = total moves and the worklist is empty 
  //   or 2. all cells in this board are the same color
  public WorldEnd worldEnds() {
    if (this.moves == this.totalMoves && this.worklist.size() == 0 || this.allSameColor()) {
      return new WorldEnd(true, this.lastScene());
    } else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  //resets processed boolean to false for all
  //cells in the board except for the first 
  void resetProcessed() {
    Iterator<ArrayList<Cell>> it = this.board.iterator();
    while (it.hasNext()) {
      ArrayList<Cell> currentRow = it.next();
      Iterator<Cell> currentIt = currentRow.iterator();
      while (currentIt.hasNext()) {
        Cell currentCell = currentIt.next();
        currentCell.resetFlag();
      }
    }
    this.board.get(0).get(0).processed = true;
  }

  //returns the last scene which indicates if the player wins or not
  public WorldScene lastScene() {
    String statement = "lose: " + this.moves  + "/" + this.totalMoves;
    if (this.allSameColor()) {
      statement = "win: " + this.moves  + "/" + this.totalMoves;
    }

    WorldImage whiteBack = new RectangleImage(this.scaleFactor * size, this.scaleFactor * 2,
        OutlineMode.SOLID, Color.WHITE)
        .movePinhole(-this.scaleFactor * size / 2, - this.scaleFactor);
    this.gameScene.placeImageXY(whiteBack, 0, this.size * scaleFactor);


    WorldImage result = new TextImage(statement, this.size * 2, Color.black);
    this.gameScene.placeImageXY(
        result, this.scaleFactor * size / 3, this.scaleFactor * size + 20);

    return this.gameScene;
  }

}

//tests and examples for FloodItWorld and Cells
class ExamplesGame {
  ExamplesGame() {}

  //FloodItWorld Examples
  FloodItWorld world10;

  ICells leaf1;

  Cell c1Left = new Cell(0, 0, Color.PINK, false, null, null, null, null);
  Cell c1Right = new Cell(0, 0, Color.RED, false, null, null, null, null);
  Cell c1Top = new Cell(0, 0, Color.GREEN, false, null, null, null, null);
  Cell c1Bottom = new Cell(0, 0, Color.CYAN, false, null, null, null, null);
  Cell c1BottomT = new Cell(0, 0, Color.BLACK, true, null, null, null, null);

  //cell w non-flooded neighbors, but matches color
  Cell cell1 = new Cell(0, 0, Color.BLUE, true, 
      this.c1Left, this.c1Top, this.c1Right, this.c1Bottom);
  ArrayList<Cell> l1 = new ArrayList<Cell>(Arrays.asList(cell1));
  //cell w 1 flooded neighbor
  Cell cell2 = new Cell(0, 0, Color.BLUE, true, 
      this.c1Left, this.c1Top, this.c1Right, this.c1BottomT);
  ArrayList<Cell> l2 = new ArrayList<Cell>(Arrays.asList(cell2));

  FloodItWorld randomW;
  FloodItWorld randomWorldTwo;
  FloodItWorld randomWSame;

  ArrayList<Cell> worklist1;
  ArrayList<Cell> worklist2;



  Cell c1 = new Cell(0, 0, Color.red, true, new LeafCell(), new LeafCell(), this.c2, this.c3);
  Cell c2 =  new Cell(0, this.scaleFactor1, Color.red, false, this.c1, 
      new LeafCell(), new LeafCell(), this.c4);
  Cell c3 = new Cell(this.scaleFactor1, 0, Color.red, false, new LeafCell(), this.c1, this.c4, 
      new LeafCell());
  Cell c4  = new Cell(this.scaleFactor1, this.scaleFactor1, Color.blue, 
      false, this.c3, this.c2, new LeafCell(), new LeafCell());

  Cell c5 = new Cell(this.scaleFactor1, this.scaleFactor1, Color.blue, 
      false, this.c3, this.c2, new LeafCell(), new LeafCell());

  Cell c6 =  new Cell(0, this.scaleFactor1, Color.red, false, this.c1, 
      new LeafCell(), new LeafCell(), this.c4);

  Cell c7 =  new Cell(0, this.scaleFactor1, Color.orange, false,  new LeafCell(), 
      new LeafCell(), new LeafCell(),  new LeafCell());


  ArrayList<Cell> r1 = new ArrayList<Cell>(Arrays.asList(this.c1, this.c2));
  ArrayList<Cell> r2 = new ArrayList<Cell>(Arrays.asList(this.c3, this.c4));
  ArrayList<ArrayList<Cell>> makeBoardTest1 = new ArrayList<ArrayList<Cell>>(
      Arrays.asList(this.r1, this.r2));

  int scaleFactor1 = 20;

  //Cell examples
  Cell cellOne;
  Cell cellTwo;
  Cell cellThree;
  Cell cellFour;
  Cell cellFive;
  Cell cellSix;
  Cell cellSeven;
  Cell cellEight;
  Cell cellNine;
  Cell cellTen;
  Cell cellEleven;
  Cell cellTwelve;
  Cell cellThirteen;
  Cell cellFourteen;


  //examples rows
  ArrayList<Cell> rowOne;
  ArrayList<Cell> rowTwo;
  ArrayList<Cell> rowThree;
  ArrayList<Cell> rowTest1;
  ArrayList<Cell> rowTest2;



  //examples columns
  ArrayList<Cell> columnOne;
  ArrayList<Cell> columnTwo;
  ArrayList<Cell> columnThree;

  //examples boards
  ArrayList<ArrayList<Cell>> boardOne;  
  ArrayList<ArrayList<Cell>> boardTwo; 
  ArrayList<ArrayList<Cell>> boardThree; 
  ArrayList<ArrayList<Cell>> board3Row; 
  ArrayList<ArrayList<Cell>> boardTest;  

  //FloodItWorld Examples
  FloodItWorld worldOne;
  FloodItWorld worldTwo;
  FloodItWorld worldThree;
  FloodItWorld world3Row;
  FloodItWorld worldRandom;
  FloodItWorld randomW2;
  FloodItWorld randomW3;



  //list of color example
  ArrayList<Color> possibleColors;


  Cell c8  = new Cell(this.scaleFactor1, this.scaleFactor1, Color.pink, 
      false, this.c7, this.c6, new LeafCell(), new LeafCell());

  ArrayList<Cell> r3 = new ArrayList<Cell>(Arrays.asList(this.c5, this.c6));
  ArrayList<Cell> r4 = new ArrayList<Cell>(Arrays.asList(this.c7, this.c8));
  ArrayList<ArrayList<Cell>> makeBoardTest2 = new ArrayList<ArrayList<Cell>>(
      Arrays.asList(this.r3, this.r4));

  Cell single = new Cell(0, 0, Color.red, true, new LeafCell(), new LeafCell(), 
      new LeafCell(), new LeafCell());
  ArrayList<Cell> r5 = new ArrayList<Cell>(Arrays.asList(this.single));
  ArrayList<ArrayList<Cell>> makeBoardTest3 = new ArrayList<ArrayList<Cell>>(
      Arrays.asList(this.r5));


  //to initialize the examples to their initial values
  void initData() {
    this.world10 = new FloodItWorld(10, 6);

    this.cell1 = new Cell(0, 0, Color.BLUE, true, 
        this.c1Left, this.c1Top, this.c1Right, this.c1Bottom);
    this.l1 = new ArrayList<Cell>(Arrays.asList(cell1));

    this.cell2 = new Cell(0, 0, Color.BLUE, true, 
        this.c1Left, this.c1Top, this.c1Right, this.c1BottomT);
    this.l2 = new ArrayList<Cell>(Arrays.asList(cell2));

    this.c1Left = new Cell(0, 0, Color.PINK, false, null, null, null, null);
    this.c1Right = new Cell(0, 0, Color.RED, false, null, null, null, null);
    this.c1Top = new Cell(0, 0, Color.GREEN, false, null, null, null, null);
    this.c1Bottom = new Cell(0, 0, Color.CYAN, false, null, null, null, null);
    this.c1BottomT = new Cell(0, 0, Color.BLACK, true, null, null, null, null);
    this.randomW = new FloodItWorld(2, 3, new Random(3));
    this.randomWorldTwo = new FloodItWorld(3, 4, new Random(4));
    this.randomWSame = new FloodItWorld(10, 1, new Random(1));


    this.leaf1 = new LeafCell();

    this.worklist1 = new ArrayList<Cell>();
    this.worklist2 = new ArrayList<Cell>(Arrays.asList(this.cell1, this.cell2));

    this.c1.x = 0;
    this.c1.y = 0;
    this.c1.color = Color.pink;
    this.c1.flooded = true;
    this.c1.left = new LeafCell();
    this.c1.top = new LeafCell();
    this.c1.right = this.c2;
    this.c1.bottom = this.c3;

    this.c2.x = 0;
    this.c2.y = this.scaleFactor1;
    this.c2.color = Color.pink;
    this.c2.flooded = false;
    this.c2.left = this.c1;
    this.c2.top = new LeafCell();
    this.c2.right = new LeafCell();
    this.c2.bottom = this.c4;

    this.c3.x = this.scaleFactor1;
    this.c3.y = 0;
    this.c3.color = Color.red;
    this.c3.flooded = false;
    this.c3.left = new LeafCell();
    this.c3.top = this.c1;
    this.c3.right = this.c4;
    this.c3.bottom = new LeafCell();

    this.c4.x = this.scaleFactor1;
    this.c4.y = this.scaleFactor1;
    this.c4.color = Color.blue;
    this.c4.flooded = false;
    this.c4.left = this.c3;
    this.c4.top = this.c2;
    this.c4.right = new LeafCell();
    this.c4.bottom = new LeafCell();

    this.c5.x = this.scaleFactor1;
    this.c5.y = this.scaleFactor1;
    this.c5.color = Color.red;
    this.c5.flooded = false;
    this.c5.left = this.cell2;
    this.c5.top = this.cell1;
    this.c5.right = new LeafCell();
    this.c5.bottom = new LeafCell();

    this.c6.x = this.scaleFactor1;
    this.c6.y = this.scaleFactor1;
    this.c6.color = Color.blue;
    this.c6.flooded = false;
    this.c6.left = this.c1;
    this.c6.top = new LeafCell();
    this.c6.right = new LeafCell();
    this.c6.bottom = this.c4;

    this.c7.x = this.scaleFactor1;
    this.c7.y = this.scaleFactor1;
    this.c7.color = Color.orange;
    this.c7.flooded = false;
    this.c7.left = new LeafCell();
    this.c7.top = new LeafCell();
    this.c7.right = new LeafCell();
    this.c7.bottom = new LeafCell();



    this.r1 = new ArrayList<Cell>(Arrays.asList(this.c1, this.c2));
    this.r2 = new ArrayList<Cell>(Arrays.asList(this.c3, this.c4));
    this.makeBoardTest1 = new ArrayList<ArrayList<Cell>>(Arrays.asList(this.r1, this.r2));



    // FROM PART ONE:


    this.scaleFactor1 = 20;
    this.possibleColors = new ArrayList<Color>(Arrays.asList(
        Color.red, Color.blue, Color.pink, Color.black,
        Color.orange, Color.yellow, Color.gray, Color.green));


    this.cellOne = new Cell(0, 0, Color.blue);
    this.cellTwo = new Cell(1,1, Color.orange);
    this.cellThree = new Cell(1, 0, Color.red);

    this.cellFour = new Cell(0, 1, Color.red);
    this.cellFive = new Cell(5, 12, Color.pink);
    this.cellSix = new Cell(6, 12, Color.pink);
    this.cellSeven = new Cell(7, 12, Color.orange);
    this.cellEight = new Cell(1, 2, Color.black);
    this.cellNine = new Cell(5, 11, Color.black);
    this.cellTen = new Cell(5, 10, Color.black);
    this.cellEleven = new Cell(0, 3, Color.black);
    this.cellTwelve = new Cell(0, 2, Color.black);
    this.cellThirteen = new Cell(2, 0, Color.black);
    this.cellFourteen = new Cell(2, 1, Color.black);


    this.rowOne = new ArrayList<Cell>(Arrays.asList(
        this.cellThree, this.cellTwo, this.cellEight));
    this.rowTwo = new ArrayList<Cell>(Arrays.asList(
        this.cellFive, this.cellNine, this.cellTen));
    this.rowThree = new ArrayList<Cell>(Arrays.asList(
        this.cellOne, this.cellFour, this.cellEight));

    this.rowTest1 =  new ArrayList<Cell>(Arrays.asList(
        this.cellOne, this.cellThree, this.cellThirteen));

    this.rowTest2 =  new ArrayList<Cell>(Arrays.asList(
        this.cellFour, this.cellTwo, this.cellFourteen));



    this.columnOne = new ArrayList<Cell>(Arrays.asList(
        this.cellFive, this.cellSix, this.cellSeven));
    this.columnTwo = new ArrayList<Cell>(Arrays.asList(
        this.cellFour, this.cellEleven, this.cellTwelve));
    this.columnThree = new ArrayList<Cell>(Arrays.asList(
        this.cellFour, this.cellEleven, this.cellTwelve));




    this.cellOne.flooded = true;
    this.cellTwo.flooded = true;
    this.cellThree.flooded = true;
    this.cellThree.flooded = true;
    this.cellFour.flooded = false;
    this.cellFive.flooded = false;
    this.cellSix.flooded = false;

    this.worldOne = new FloodItWorld(2,3);

    this.worldTwo = new FloodItWorld(3,6);

    this.worldThree = new FloodItWorld(5, 7);

    this.world3Row = new FloodItWorld(12,6);

    this.boardOne = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(this.rowOne, this.columnTwo));  

    this.boardTwo = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(this.rowTwo, this.columnOne));  

    this.boardThree = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(this.rowThree, this.columnThree));  


    this.board3Row = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(this.rowThree, this.rowTwo, this.rowOne)); 

    this.boardTest = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(this.rowTest1, this.rowTest2)); 


    this.worldOne.board = this.boardOne;
    this.worldTwo.board = this.boardTwo;
    this.worldThree.board = this.boardThree;
    this.world3Row.board = this.board3Row;
    this.world10 = new FloodItWorld(10, 6);


    this.worldRandom = new FloodItWorld(10, 3);

    this.possibleColors = new ArrayList<Color>(Arrays.asList(
        Color.red, Color.blue, Color.pink, Color.black,
        Color.orange, Color.yellow, Color.gray, Color.green));


    this.randomW2 = new FloodItWorld(2, 6, new Random(21));

    this.randomW3 = new FloodItWorld(1, 4, new Random(1));

    this.c8.x = this.scaleFactor1;
    this.c8.y = this.scaleFactor1;
    this.c8.color = Color.pink;
    this.c8.flooded = false;
    this.c8.left = this.c7;
    this.c8.top = this.c6;
    this.c8.right = new LeafCell();
    this.c8.bottom = new LeafCell();

    this.single = new Cell(0, 0, Color.red, true, new LeafCell(), new LeafCell(),
        new LeafCell(), new LeafCell());
    this.r5 = new ArrayList<Cell>(Arrays.asList(this.single));
    this.makeBoardTest3 = new ArrayList<ArrayList<Cell>>(Arrays.asList(this.r5));


  }

  //to initialize the linkage in our boardTest to link the board's cells for testing
  void initLink() {
    this.initData();
    for (int i = 0; i < 1; i++) {
      for (int j = 0; j < 3; j++) {

        if (j + 1 < 3) { 
          this.boardTest.get(i).get(j).right = this.boardTest.get(i).get(j + 1);
        }
        if (j - 1 >= 0) { 
          this.boardTest.get(i).get(j).left = this.boardTest.get(i).get(j - 1);

        }
        if (i + 1 < 3) { 
          this.boardTest.get(i).get(j).bottom = this.boardTest.get(i + 1).get(j);
        }
        if (i - 1 >= 0) { 
          this.boardTest.get(i).get(j).top = this.boardTest.get(i - 1).get(j);
        }
      }

    }
  }




  //// to test the method testResetProcessed

  boolean testResetProcessed(Tester t) {
    this.initData();

    this.cellFour.processed = true;
    this.cellEleven.processed = true;
    this.cellTwelve.processed = true;

    this.cellThree.processed = true;
    this.cellTwo.processed = true;


    boolean initialConditions =
        t.checkExpect(this.cellFour.processed, true)
        && t.checkExpect(this.cellEleven.processed, true)
        && t.checkExpect(this.cellThree.processed, true)
        && t.checkExpect(this.cellTwo.processed, true)
        && t.checkExpect(this.cellEight.processed, false)
        && t.checkExpect(this.cellTwelve.processed, true);

    this.worldOne.resetProcessed();

    boolean finalConditions =
        t.checkExpect(this.cellFour.processed, false)
        && t.checkExpect(this.cellEleven.processed, false)
        && t.checkExpect(this.cellThree.processed, false)
        && t.checkExpect(this.cellTwo.processed, false)
        && t.checkExpect(this.cellEight.processed, false)
        && t.checkExpect(this.cellTwelve.processed, false);

    return initialConditions && finalConditions;

  }

  //tests the method drawCell
  void testDrawCell(Tester t) {
    this.initData();
    WorldImage cellOneHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.blue)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellTwoHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.orange)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellThreeHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.red)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellFourHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.red)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellEightHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellElevenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellTwelveHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellFiveHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.pink)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellNineHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellTenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellSixHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.pink)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellSevenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.orange)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);

    t.checkExpect(this.cellOne.drawCell(this.scaleFactor1), cellOneHardImg);
    t.checkExpect(this.cellTwo.drawCell(this.scaleFactor1), cellTwoHardImg);
    t.checkExpect(this.cellThree.drawCell(this.scaleFactor1), cellThreeHardImg);
    t.checkExpect(this.cellFour.drawCell(this.scaleFactor1), cellFourHardImg);
    t.checkExpect(this.cellEight.drawCell(this.scaleFactor1), cellEightHardImg);
    t.checkExpect(this.cellEleven.drawCell(this.scaleFactor1), cellElevenHardImg);
    t.checkExpect(this.cellTwelve.drawCell(this.scaleFactor1), cellTwelveHardImg);
    t.checkExpect(this.cellFive.drawCell(this.scaleFactor1), cellFiveHardImg);
    t.checkExpect(this.cellNine.drawCell(this.scaleFactor1), cellNineHardImg);
    t.checkExpect(this.cellTen.drawCell(this.scaleFactor1), cellTenHardImg);
    t.checkExpect(this.cellSix.drawCell(this.scaleFactor1), cellSixHardImg);
    t.checkExpect(this.cellSeven.drawCell(this.scaleFactor1), cellSevenHardImg);
  }

  //to test the method makeScene
  //test the method makeScene
  void testMakeScene(Tester t) {
    this.initData();

    // hard code to match what is supposed to be the scene of worldOne
    WorldScene gameBoardImg = new WorldScene(40, 40 + 40);


    WorldImage scoreBox1 = new RectangleImage(
        this.scaleFactor1 * 40, 40, OutlineMode.SOLID, Color.white)
        .movePinhole(- this.scaleFactor1, - this.scaleFactor1);


    // WorldImage cellOneImg = this.cellOne.drawCell(20);
    WorldImage cellOneHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.blue)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    // WorldImage cellTwoImg = this.cellTwo.drawCell(20);
    WorldImage cellTwoHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.orange)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    //WorldImage cellThreeImg = this.cellThree.drawCell(20);
    WorldImage cellThreeHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.red)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    // WorldImage cellFourImg = this.cellFour.drawCell(20);
    WorldImage cellFourHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.red)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    //WorldImage cellEightImg = this.cellEight.drawCell(20);
    WorldImage cellEightHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    // WorldImage cellElevenImg = this.cellEleven.drawCell(20);
    WorldImage cellElevenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    // WorldImage cellTwelveImg = this.cellTwelve.drawCell(20);
    WorldImage cellTwelveHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1, 
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);

    WorldImage gameBoardImg1Score = new TextImage("moves: 0/2", this.scaleFactor1, Color.BLACK);

    gameBoardImg.placeImageXY(cellThreeHardImg, 1 ,  0);
    gameBoardImg.placeImageXY(cellTwoHardImg, 1 , 1);
    gameBoardImg.placeImageXY(cellEightHardImg, 1 , 2 );

    gameBoardImg.placeImageXY(cellFourHardImg, 0 , 1);
    gameBoardImg.placeImageXY(cellElevenHardImg, 0, 3 );
    gameBoardImg.placeImageXY(cellTwelveHardImg, 0 , 2 );
    gameBoardImg.placeImageXY(scoreBox1, 0 , 40 );
    gameBoardImg.placeImageXY(gameBoardImg1Score, 20 , 60);


    // hard code to match what is supposed to be the scene of worldTwo
    WorldScene gameBoardImg2 = new WorldScene(60, 60 + 40);

    WorldImage cellFiveHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.pink)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellNineHardImg = new RectangleImage(this.scaleFactor1,this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellTenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.black)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellSixHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.pink)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);
    WorldImage cellSevenHardImg = new RectangleImage(this.scaleFactor1, this.scaleFactor1,
        OutlineMode.SOLID, Color.orange)
        .movePinhole(- this.scaleFactor1 / 2, - this.scaleFactor1 / 2);

    WorldImage gameBoardImg2Score = new TextImage("moves: 0/6", this.scaleFactor1, Color.BLACK);
    WorldImage scoreBox2 = new RectangleImage(
        this.scaleFactor1 * 60, 40, OutlineMode.SOLID, Color.white)
        .movePinhole(- this.scaleFactor1, - this.scaleFactor1);

    WorldImage gameBoardImg3Score = new TextImage("moves: 0/11", this.scaleFactor1, Color.BLACK);
    WorldImage scoreBox3 = new RectangleImage(
        this.scaleFactor1 * 100, 40, OutlineMode.SOLID, Color.white)
        .movePinhole(- this.scaleFactor1, - this.scaleFactor1);

    WorldImage gameBoardImg3RowScore = new TextImage(
        "moves: 0/24", this.scaleFactor1, Color.BLACK);
    WorldImage scoreBox3Row = new RectangleImage(
        this.scaleFactor1 * 240, 40, OutlineMode.SOLID, Color.white)
        .movePinhole(- this.scaleFactor1, - this.scaleFactor1);

    gameBoardImg2.placeImageXY(cellFiveHardImg, 5 , 12 );
    gameBoardImg2.placeImageXY(cellNineHardImg, 5 , 11 );
    gameBoardImg2.placeImageXY(cellTenHardImg, 5 , 10 );

    gameBoardImg2.placeImageXY(cellFiveHardImg, 5 , 12 );
    gameBoardImg2.placeImageXY(cellSixHardImg, 6 , 12);
    gameBoardImg2.placeImageXY(cellSevenHardImg, 7  , 12 );
    gameBoardImg2.placeImageXY(scoreBox2, 0 , 60);
    gameBoardImg2.placeImageXY(gameBoardImg2Score, 30, 80);


    // hard code to match what is supposed to be the scene of worldThree
    WorldScene gameBoardImg3 = new WorldScene(100, 100 + 40 );

    gameBoardImg3.placeImageXY(cellOneHardImg, 0 , 0  );
    gameBoardImg3.placeImageXY(cellFourHardImg, 0, 1);
    gameBoardImg3.placeImageXY(cellEightHardImg, 1 , 2 );

    gameBoardImg3.placeImageXY(cellFourHardImg, 0 , 1 ) ;
    gameBoardImg3.placeImageXY(cellElevenHardImg, 0 , 3 );
    gameBoardImg3.placeImageXY(cellTwelveHardImg, 0 , 2 );
    gameBoardImg3.placeImageXY(scoreBox3, 0 , 100);
    gameBoardImg3.placeImageXY(gameBoardImg3Score, 50, 120);

    // hard code to match what is supposed to be the scene of world3Row
    WorldScene gameBoard3Row = new WorldScene(240, 240 + 40);

    gameBoard3Row.placeImageXY(cellOneHardImg, 0, 0 );
    gameBoard3Row.placeImageXY(cellFourHardImg, 0, 1 );
    gameBoard3Row.placeImageXY(cellEightHardImg, 1, 2 );

    gameBoard3Row.placeImageXY(cellFiveHardImg, 5, 12 );
    gameBoard3Row.placeImageXY(cellNineHardImg, 5, 11);
    gameBoard3Row.placeImageXY(cellTenHardImg, 5, 10 );

    gameBoard3Row.placeImageXY(cellThreeHardImg, 1, 0 );
    gameBoard3Row.placeImageXY(cellTwoHardImg, 1, 1);
    gameBoard3Row.placeImageXY(cellEightHardImg, 1, 2);
    gameBoard3Row.placeImageXY(scoreBox3Row, 0 , 240 );
    gameBoard3Row.placeImageXY(gameBoardImg3RowScore, 120, 260);



    t.checkExpect(worldOne.makeScene(), gameBoardImg);
    t.checkExpect(worldTwo.makeScene(), gameBoardImg2);
    t.checkExpect(worldThree.makeScene(), gameBoardImg3);
    t.checkExpect(world3Row.makeScene(), gameBoard3Row);


  }

  //test that linkCells link the cells together in the board
  void testLinkCells(Tester t) {
    this.initData();


    t.checkExpect(this.cellOne.top, new LeafCell());

    this.initLink();
    t.checkExpect(this.cellOne.right, this.cellThree);
    t.checkExpect(this.cellOne.left, new LeafCell());
    t.checkExpect(this.cellOne.top, new LeafCell() );
    t.checkExpect(this.cellOne.bottom, this.cellFour);
    t.checkExpect(this.cellThree.bottom, this.cellTwo);
    t.checkExpect(this.cellThree.right, this.cellThirteen);



  }

  //tests the random color of the cells created
  boolean testGetNewRandomColor(Tester t) {
    this.initData();
    FloodItWorld testRandWorld = new FloodItWorld(3, 3, new Random(3));
    return t.checkExpect(testRandWorld.getNewRandomColor(), Color.blue)
        && t.checkExpect(testRandWorld.getNewRandomColor(), Color.red)
        && t.checkExpect(testRandWorld.getNewRandomColor(), Color.blue)
        && t.checkExpect(testRandWorld.getNewRandomColor(), Color.pink);

  }

  //test the make board method
  boolean testMakeBoard(Tester t) {
    this.initData();

    ArrayList<ArrayList<Cell>> testBoard1 = new ArrayList<ArrayList<Cell>>();
    ArrayList<ArrayList<Cell>> testBoard2 = new ArrayList<ArrayList<Cell>>();

    FloodItWorld newWorldTest = new FloodItWorld(3,6);
    newWorldTest.board = testBoard1;

    FloodItWorld newWorldTest2 = new FloodItWorld(3,6);
    newWorldTest2.board = testBoard2;

    boolean initialConditions = 
        t.checkExpect(newWorldTest.board.size(), 0)
        &&  t.checkExpect(this.worldOne.board.size(), 2)
        &&  t.checkExpect(newWorldTest2.board.size(), 0);

    testBoard1.add(this.r1);
    testBoard1.add(this.r2);
    newWorldTest.board = testBoard1;

    this.boardOne.add(this.r2);

    testBoard2.add(this.r5);



    boolean finalConditions = 
        t.checkExpect(newWorldTest.board.size(), 2)
        &&  t.checkExpect(this.worldOne.board.size(), 3)
        &&  t.checkExpect(newWorldTest2.board.size(), 1);


    return initialConditions && finalConditions;
  }







  //to test the method addToWorklist
  boolean testAddToWorklist(Tester t) {
    ArrayList<Cell> workList = new ArrayList<Cell>();
    ArrayList<Cell> workListTwo = new ArrayList<Cell>();

    this.initData();




    boolean initalConditions =
        t.checkExpect(workList.size(), 0)
        &&  t.checkExpect(workListTwo.size(), 0);

    this.leaf1.addToWorklist(Color.black, workList);
    this.c5.top.addToWorklist(Color.pink, workListTwo);
    this.c5.left.addToWorklist(Color.pink, workListTwo);
    this.c7.left.addToWorklist(Color.orange, workListTwo);



    boolean finalConditions =
        t.checkExpect(workList.size(), 0)
        && t.checkExpect(workListTwo.size(), 2);



    return initalConditions && finalConditions;

  }


  //to test the method changeColor
  boolean testChangeColor(Tester t) {
    this.initData(); 

    boolean initalConditions =
        t.checkExpect(this.c1.color, Color.pink)
        && t.checkExpect(this.c3.color, Color.red)
        && t.checkExpect(this.c4.color, Color.blue);

    this.c1.changeColor(Color.black);
    this.c3.changeColor(Color.pink);
    this.c4.changeColor(Color.yellow);

    boolean finalConditions =
        t.checkExpect(this.c1.color, Color.black)
        && t.checkExpect(this.c3.color, Color.pink)
        && t.checkExpect(this.c4.color, Color.yellow);

    return initalConditions && finalConditions;

  }


  //to test the method allSameColor
  boolean testAllSameColor(Tester t) {
    this.initData();
    return t.checkExpect(this.randomW.allSameColor(), false)
        && t.checkExpect(this.randomWorldTwo.allSameColor(), false)
        && t.checkExpect(this.randomWSame.allSameColor(), true);

  }

  //to test the method sameColor
  boolean testSameColor(Tester t) {
    this.initData();
    // return t.checkExpect(this.c5.sameColor(Color.red), true)
    return  t.checkExpect(this.c4.sameColor(Color.black), false)
        && t.checkExpect(this.c3.sameColor(Color.red), true)
        && t.checkExpect(this.c1.sameColor(Color.pink), true);
  }


  //to test the method process
  boolean testProcess(Tester t) {
    ArrayList<Cell> workList = new ArrayList<Cell>();
    ArrayList<Cell> workListTwo = new ArrayList<Cell>();
    ArrayList<Cell> workListThree = new ArrayList<Cell>();
    this.initData();



    boolean initialConditions = 
        t.checkExpect(workList.size(), 0)
        && t.checkExpect(workListTwo.size(), 0)
        && t.checkExpect(workListThree.size(), 0);


    this.c5.process(Color.red, workList);
    this.c6.process(Color.blue, workListTwo);
    this.c7.process(Color.blue, workListThree);


    boolean finalConditions = 
        t.checkExpect(workList.size(), 2)
        && t.checkExpect(workListTwo.size(), 2)
        && t.checkExpect(workListThree.size(), 0);


    return initialConditions && finalConditions;

  }

  //to test the method resetFlag
  boolean testResetFlag(Tester t) {
    this.initData();
    this.c4.processed = true;
    this.c1.processed = true;
    this.c2.processed = false;
    boolean initialConditions = 
        t.checkExpect(this.c4.processed, true)
        && t.checkExpect(this.c1.processed, true)
        && t.checkExpect(this.c2.processed, false);

    this.c4.resetFlag();
    this.c1.resetFlag();
    this.c2.resetFlag();

    boolean finalConditions =
        t.checkExpect(this.c4.processed, false)
        && t.checkExpect(this.c1.processed, false)
        && t.checkExpect(this.c2.processed, false);

    return initialConditions && finalConditions;
  }





  //to test the random generation of a FloodItBoard
  void testGame2(Tester t) {
    this.initData();
    int boardSize = this.world10.size * this.world10.scaleFactor;
    this.world10.bigBang(boardSize, boardSize + 40, .01);
  }


}

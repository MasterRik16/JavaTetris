# Java Tetris

A classic Tetris game written in Java with Swing. One source file, no dependencies.

## Features

- All 7 tetrominoes, dealt with the 7-bag randomizer
- Rotation in both directions with simple wall kicks
- Ghost piece showing where the piece will land
- Next-piece preview
- Scoring that scales with level, plus soft-drop and hard-drop bonuses
- Level increases every 10 lines, and gravity speeds up with each level
- Pause and restart

## Requirements

- JDK 8 or newer (`javac` and `java` on your PATH)

## Build and run

From the project root:

```bash
javac -d out src/Tetris.java
java -cp out Tetris
```

## Controls

| Key           | Action                   |
|---------------|--------------------------|
| Left / Right  | Move                     |
| Down          | Soft drop                |
| Up or X       | Rotate clockwise         |
| Z             | Rotate counter-clockwise |
| Space         | Hard drop                |
| P             | Pause / resume           |
| R             | Restart                  |

## Scoring

| Lines cleared at once | Points            |
|-----------------------|-------------------|
| 1                     | 100 x level       |
| 2                     | 300 x level       |
| 3                     | 500 x level       |
| 4                     | 800 x level       |

Soft drop gives 1 point per cell, and hard drop gives 2 points per cell.

## Project structure

```
java-tetris/
├── src/
│   └── Tetris.java   # game logic, input handling and rendering
├── .gitignore
└── README.md
```

## Ideas for improvement

- Hold piece
- Lock delay
- High-score saving
- Sound effects
- Split the game rules into a separate class from the Swing UI so they can be unit tested

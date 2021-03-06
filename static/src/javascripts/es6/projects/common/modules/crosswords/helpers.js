import _ from 'common/utils/_';

import constants from 'es6/projects/common/modules/crosswords/constants';

const getLastCellInClue = (clue) => {
    const ax = { true: 'x', false: 'y' };
    const axis = ax[isAcross(clue)];
    const otherAxis = ax[!isAcross(clue)];
    return {
        [axis]: clue.position[axis] + (clue.length - 1),
        [otherAxis]: clue.position[otherAxis]
    };
};

const isFirstCellInClue = (cell, clue) => {
    const axis = isAcross(clue) ? 'x' : 'y';
    return cell[axis] === clue.position[axis];
};

const isLastCellInClue = (cell, clue) => {
    const axis = isAcross(clue) ? 'x' : 'y';
    return cell[axis] === clue.position[axis] + (clue.length - 1);
};

const getNextClueInGroup = (entries, clue) => {
    const newClueId = clue.group[_.findIndex(clue.group, id => id === clue.id) + 1];
    return _.find(entries, { id: newClueId });
};

const getPreviousClueInGroup = (entries, clue) => {
    const newClueId = clue.group[_.findIndex(clue.group, id => id === clue.id) - 1];
    return _.find(entries, { id: newClueId });
};

const isAcross = (clue) => clue.direction === 'across';

const otherDirection = (direction) => direction === 'across' ? 'down' : 'across';

const cellsForEntry = (entry) => isAcross(entry) ?
    _.map(_.range(entry.position.x, entry.position.x + entry.length), (x) => ({
        x: x,
        y: entry.position.y
    })) :
    _.map(_.range(entry.position.y, entry.position.y + entry.length), (y) => ({
        x: entry.position.x,
        y: y
    }));

/**
 * Builds the initial state of the grid given the number of rows, columns, and a list of clues.
 */
const buildGrid = (rows, columns, entries, savedState) => {
    const grid = _.map(_.range(columns), (x) => _.map(_.range(rows), (y) => ({
        isHighlighted: false,
        isEditable: false,
        isError: false,
        isAnimating: false,
        value: (savedState && savedState[x] && savedState[x][y]) ? savedState[x][y] : ''
    })));

    _.forEach(entries, (entry) => {
        const x = entry.position.x;
        const y = entry.position.y;

        grid[x][y].number = entry.number;

        _.forEach(cellsForEntry(entry), (cell) => {
            grid[cell.x][cell.y].isEditable = true;
        });
    });

    return grid;
};

/** Hash key for the cell at x, y in the clue map */
const clueMapKey = (x, y) => `${x}_${y}`;

/** A map for looking up clues that a given cell relates to */
const buildClueMap = (clues) => {
    const map = {};

    _.forEach(clues, (clue) => {
        _.forEach(cellsForEntry(clue), (cell) => {
            const key = clueMapKey(cell.x, cell.y);

            if (map[key] === undefined) {
                map[key] = {};
            }

            if (isAcross(clue)) {
                map[key].across = clue;
            } else {
                map[key].down = clue;
            }
        });
    });

    return map;
};

/** A map for looking up separators (i.e word or hyphen) that a given cell relates to */
const buildSeparatorMap = (clues) =>
    _(clues)
        .map((clue) =>
            _.map(clue.separatorLocations, (locations, separator) =>
                locations.map(location => {
                    const key = isAcross(clue)
                        ? clueMapKey(clue.position.x + location, clue.position.y)
                        : clueMapKey(clue.position.x, clue.position.y + location);

                    return {
                        key,
                        direction: clue.direction,
                        separator
                    };
                })
            )
        )
        .flatten()
        .reduce((map, d) => {
            if (map[d.key] === undefined) {
                map[d.key] = {};
            }

            map[d.key][d.direction] = d.separator;

            return map;
        }, {});

const entryHasCell = (entry, x, y) => _.any(cellsForEntry(entry), (cell) => cell.x === x && cell.y === y);

/** Can be used for width or height, as the cell height == cell width */
const gridSize = (cells) => cells * (constants.cellSize + constants.borderSize) + constants.borderSize;

const mapGrid = (grid, f) => _.map(grid, (col, x) => {
    return _.map(col, (cell, y) => {
        return f(cell, x, y);
    });
});

export default {
    isAcross: isAcross,
    otherDirection: otherDirection,
    buildGrid: buildGrid,
    clueMapKey: clueMapKey,
    buildClueMap: buildClueMap,
    buildSeparatorMap,
    cellsForEntry: cellsForEntry,
    entryHasCell: entryHasCell,
    gridSize: gridSize,
    mapGrid: mapGrid,
    getLastCellInClue,
    isFirstCellInClue,
    isLastCellInClue,
    getNextClueInGroup,
    getPreviousClueInGroup
};

import java.util.*;
import java.util.stream.*;
import java.util.ArrayList;

public class MainClass {
    static int tick = 0;
    static Direction nextDir;
    static int package_id;
    static Reader reader;
    static ArrayList<Data> ourData;

    public static void main(String[] args) {
        final String teamToken = "85vcxibZoz9LPTKRMDWn";
        final int seed = -896560976; //new Random().nextInt();

        System.out.println("START " + teamToken + " " + seed + " version");

        reader = new Reader();

        nextDir = Direction.LEFT;
        package_id = -1;

        String command;

        while (true) {
            System.err.println("TICK " + tick);
            readData(reader);
            ourData = getOurData();
            if (reader.hasEnd && !isThereAnyDataOutThere(ourData)) break;

            if (ourData.size() < 4 && !reader.hasEnd) {
                command = createNewPackage();
            } else {
                command = modifyRouter();
            }
            System.out.println(String.format("%d %d %d %s", reader.data[0], reader.data[1], reader.data[2], command));

            System.err.println();
            tick++;
        }

        System.err.println(getSolution());
        command = getSolution();

        System.out.println(String.format("%d %d %d %s", reader.data[0], reader.data[1], reader.data[2], command));
    }

    static int calculateNextPos(Data data, boolean[][] map) {
        int[] nextPos = {data.currRouter, data.currStoreId};

        while (true) {
            if (nextPos[0] == data.toRouter) {
                break;
            }
            if (nextPos[1] - 1 >= 0 && !map[nextPos[0]][nextPos[1] - 1]) {
                nextPos[1]--;
            } else if (data.dir == Direction.RIGHT
                    && !map[(nextPos[0] + 1) % 14][nextPos[1]]
                    && !isThereOpposingData((nextPos[0] + 1) % 14, nextPos[1], data.dir)) {
                nextPos[0] = (nextPos[0] + 1) % 14;
            } else if (data.dir == Direction.LEFT
                    && !map[(nextPos[0] + 13) % 14][nextPos[1]]
                    && !isThereOpposingData((nextPos[0] + 13) % 14, nextPos[1], data.dir)) {
                nextPos[0] = (nextPos[0] + 13) % 14;
            } else {
                break;
            }
        }

        return nextPos[0];
    }

    static boolean isThereOpposingData(int x, int y, Direction ownDir){
        Direction oppositeDir = (ownDir == Direction.LEFT ? Direction.RIGHT : Direction.LEFT);
        for(Data data: reader.dataArray){
            if(data.currRouter == x && data.currStoreId == y && data.dir == oppositeDir) return true;
        }

        return false;
    }

    static int getDistance(Data data, boolean[][] map) {
        return dist(data.currRouter, calculateNextPos(data, map));
    }

    static int moveEfficiency(ArrayList<Data> ourDatas, boolean[][] newMap) {
        int sum = 0;
        for (Data data : ourDatas) {
            int dist = getDistance(data, newMap);
            sum += dist;
        }

        return sum;
    }

    static boolean[][] newMap(int router, boolean isUp) {
        boolean[][] map = new boolean[14][10];
        for (int i = 0; i < 14; i++) {
            for (int j = 0; j < 10; j++) {
                map[i][j] = reader.routerBits[i][j];
            }
        }
        if (!isUp) {
            boolean lastBit = map[router][map[router].length - 1];
            for (int i = map[router].length - 1; i > 0; i--) {
                map[router][i] = map[router][i - 1];
            }
            map[router][0] = lastBit;
        } else {
            boolean firstBit = map[router][0];
            for (int i = 1; i < map[router].length; i++) {
                map[router][i - 1] = map[router][i];
            }
            map[router][map[router].length - 1] = firstBit;
        }

        return map;
    }

    static ArrayList<Data> newDatas(int router, boolean isUp) {
        ArrayList<Data> datas = new ArrayList<>();
        datas.addAll(getOurData());
        for (Data data : datas) {
            if (data.currRouter == router) {
                if (newMap(router, isUp)[data.currRouter][data.currStoreId]) {
                    if (isUp) {
                        data.currStoreId = (data.currStoreId + 9) % 10;
                    } else {
                        data.currStoreId = (data.currStoreId + 1) % 10;
                    }
                }
                break;
            }
        }

        return datas;
    }

    static boolean isOurPackage(Data data) {
        return data.fromRouter == reader.data[2];
    }

    static ArrayList<Data> getOurData() {
        ArrayList<Data> ourData = new ArrayList<>();
        for (Data data : reader.dataArray) {
            if (isOurPackage(data)) ourData.add(data);
        }
        return ourData;
    }

    static String getSolution() {
        StringBuilder solution = new StringBuilder("SOLUTION ");

        /*ArrayList<MessagePiece> orderedList = new ArrayList<>();
        for (int i = 0; i < reader.receivedPieces.size(); i++) {
            for (MessagePiece piece : reader.receivedPieces) {
                if (piece.index == i) {
                    orderedList.add(piece);
                }
            }
        }*/


        for (int i = 0; i < reader.receivedPieces.size(); i++) {
            for (MessagePiece piece : reader.receivedPieces) {
                if(piece.index == i){
                    solution.append(piece.message);
                }
            }
        }

        return solution.toString();
    }

    static String modifyRouter() {
        int max = -1;

        int bestRouter = -1;
        boolean bestDirection = true;


        boolean[] movable = new boolean[14];
        for (Data data : ourData) {
            movable[data.currRouter] = true;
        }
        for (int i = 0; i < 14; i++) {
            if (movable[i]) {
                int sum = moveEfficiency(newDatas(i, true), newMap(i, true));
                if (sum > max) {
                    max = sum;
                    bestRouter = i;
                    bestDirection = true;
                }

                sum = moveEfficiency(newDatas(i, false), newMap(i, false));
                if (sum > max) {
                    max = sum;
                    bestRouter = i;
                    bestDirection = false;
                }
            }
        }

        return "MOVE " + bestRouter + (bestDirection ? " ^" : " v");
    }

    static int dist(int a, int b) {
        return Math.min(Math.abs(a - b), Math.abs(14 - Math.abs(a - b)));
    }

    static boolean isThereAnyDataOutThere(ArrayList<Data> datas) {
        for (Data data : datas) {
            if (data.fromRouter == data.toRouter) return true;
        }

        return false;
    }

    static void printDataArray(ArrayList<Data> datas) {

        for (Data data : datas) {
            System.err.println("{id: " + data.dataIndex
                    + ", router: " + data.currRouter
                    + ", store: " + data.currStoreId + "}");
        }
    }

    static String createNewPackage() {
        int bestStoreId = 0;
        int bestDist = 0;

        for (int i = 0; i < 10; i++) {
            if(!reader.routerBits[reader.data[2]][i]){
                boolean free = true;
                for (Data data : reader.dataArray) {
                    if (data.currRouter == reader.data[2] && data.currStoreId == i) {
                        free = false;
                        break;
                    }
                }
                if(free){
                    Data data = new Data(reader.data[2], i, nextDir);
                    if(getDistance(data, reader.routerBits) > bestDist){
                        bestDist = getDistance(data, reader.routerBits);
                        bestStoreId = i;
                    }
                }
            }
        }

        if (nextDir == Direction.LEFT) nextDir = Direction.RIGHT;
        else nextDir = Direction.LEFT;
        package_id++;

        return "CREATE " + bestStoreId + " " + package_id;
    }

    static void readData(Reader reader) {
        String line;
        Scanner inScanner;
        inScanner = new Scanner(System.in);

        reader.dataArray.clear();

        while (!(line = inScanner.nextLine()).isEmpty()) {
            Scanner scanner = new Scanner(line);
            String command = scanner.next();
            switch (command) {
                case ".":
                    return;
                case "WRONG":
                case "SCORE":
                case "TICK":
                    reader.hasEnd = true;
                    reader.previous = command + " " + scanner.nextLine();
                    break;
                case "REQUEST":
                    IntStream.range(0, 3)
                            .forEach(i -> reader.data[i] = scanner.nextInt());
                    break;
                case "PREVIOUS":
                    reader.previous = scanner.nextLine();
                    break;
                case "ROUTER":
                    int routerIndex = scanner.nextInt();
                    String bits = scanner.next();
                    IntStream.range(0, 10).forEach(i -> reader.routerBits[routerIndex][i] =
                            bits.charAt(i) == '0');
                    break;
                case "DATA":
                    reader.dataArray.add(new Data(scanner));
                    break;
                case "MESSAGE":
                    MessagePiece mp = new MessagePiece(scanner);
                    if (mp.message != null) {
                        reader.receivedPieces.add(mp);
                    } else {
                        reader.hasEnd = true;
                        System.err.println("empty message");
                    }
                    break;
                default:
                    System.err.println("READER ERROR HAPPENED: unrecognized command line: " + line);
                    reader.hasEnd = true;
                    return;
            }
        }
        System.err.println("Unexpected input end\n");
        reader.hasEnd = true;
    }
}

enum Direction {
    LEFT('l'),
    RIGHT('r');

    char c;

    Direction(char c) {
        this.c = c;
    }

    public static Direction valueOf(char c) {
        for (Direction dir : values())
            if (dir.c == c)
                return dir;
        return valueOf(c + "");
    }
}

class Data {
    int currRouter;
    int currStoreId;
    int dataIndex;
    int messageId;
    int fromRouter;
    int toRouter;
    Direction dir;

    Data(Scanner scanner) {
        this.currRouter = scanner.nextInt();
        this.currStoreId = scanner.nextInt();
        this.dataIndex = scanner.nextInt();
        this.messageId = scanner.nextInt();
        this.fromRouter = scanner.nextInt();
        this.toRouter = scanner.nextInt();
        this.dir = Direction.valueOf(scanner.next("[lr]").charAt(0));
    }

    Data(int fromRouter, int currStoreId, Direction dir){
        this.fromRouter = fromRouter;
        this.currRouter = fromRouter;
        this.currStoreId = currStoreId;
        this.toRouter = (fromRouter + 7) % 14;
        this.dir = dir;
    }
}

class MessagePiece {
    int index;
    String message;

    MessagePiece(Scanner scanner) {
        if (scanner.hasNextInt()) this.index = scanner.nextInt();
        if (scanner.hasNext()) this.message = scanner.next();

        System.err.println("message: " + this.index + " " + this.message);
    }
}

class Reader {
    int[] data = new int[]{0, 0, 0};
    String previous = "";
    boolean[][] routerBits = Stream.generate(() -> new boolean[10]).limit(14).toArray(boolean[][]::new);
    ArrayList<Data> dataArray = new ArrayList<>();
    ArrayList<MessagePiece> receivedPieces = new ArrayList<>();
    boolean hasEnd = false;
}

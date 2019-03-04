package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import creator.Aiger;
import creator.Rabinizer;
import creator.Wash;
import model.Game;
import model.GameInfo;
import strategy.Strategy;
import util.Pair;
import util.Triplet;
import util.Util;
import strategy.Parity3;
import strategy.Reachability;
import strategy.Safety;
import machinelearning.BinaryDecisionDiagram;
import machinelearning.Dataset;
import machinelearning.DecisionTree;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Routine {

  /**
   * Prints information on how to use the program
   */
  public static void message() {
    System.out.println("The program expects one input string-argument:");
    System.out.println("'a'   -- creates AIGER games and solves them");
    System.out.println("'wX'  -- creates Wash games with X (2..4) tanks and solves them");
    System.out.println("'Ra'  -- represents computed AIGER strategies");
    System.out.println("'RwX' -- represents computed Wash strategies with X (2..4) tanks (X=0 for reachability)");
    System.out.println("'rabN' -- creates naive Rabinizer games, solves them and represents computed strategies");
    System.out.println("'rabE' -- creates encoded Rabinizer games, solves them and represents computed strategies");
    System.out.println("'aTOTAL' -- performs entire Bit Shifter experiments");
    System.out.println("'wTOTAL' -- performs entire Scheduling of Washing Cycles experiments");
    System.out.println("'rTOTAL' -- performs entire Random LTL experiments");
  }

  /**
   * Rabinizer:: Games -> Datasets -> BDDs and DTs
   */
  public static void rabinizer(boolean encoded) {
    GameInfo gameinfo = new GameInfo();
    gameinfo.type = 'r';
    Random seedgen = new Random(47);

    int startfile = 1;
    int endfile = 83; // 83 ... 84 to 91 require parity4+ solver

    File directory = new File("results/reports/");
    if (!directory.exists())
      directory.mkdirs();
    File outputFile = new File("results/reports/reprRandomLTL"+(encoded?"encoded":"naive")+".txt");
    String nl = System.getProperty("line.separator");
    try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {

      writer.write(String.format("%118s","|BDD|"));
      writer.write(String.format("%30s","|DTnl|"));
      writer.write(String.format("%30s","|DT|"));
      writer.write(String.format("%30s","|DT+|"));
      writer.write(nl);

      writer.write(String.format("%-30s","Name"));
      writer.write(String.format("%12s","|S|"));
      writer.write(String.format("%6s","|I|"));
      writer.write(String.format("%6s","|O|"));
      writer.write(String.format("%16s","|Good|+|Bad|"));

      writer.write(String.format("%14s","Orig"));
      writer.write(String.format("%8s","Min"));
      writer.write(String.format("%10s","Mean"));
      writer.write(String.format("%8s","Max"));
      writer.write(String.format("%8s","Time"));

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(nl);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    for (int filen=startfile; filen<=endfile; filen++) {
      System.gc();

      gameinfo.filename = "randomltl"+filen;
      int apnumber = Rabinizer.apnumber(gameinfo.filename);
      if (apnumber == -1) continue;

      for (int APassgn=1; APassgn<Util.bitpower(apnumber)-1; APassgn++) {
        Boolean[] apinfo = new Boolean[apnumber];
        int help = APassgn;
        for (int i=0; i<apnumber; i++) {
          apinfo[i] = (help % 2 == 1);
          help /= 2;
        }

        Game game = (encoded?Rabinizer.createEncoded(gameinfo.filename, apinfo)
            :Rabinizer.createNaive(gameinfo.filename, apinfo));


        File windir = new File("results/gamefiles/win");
        if (!windir.exists())
          windir.mkdirs();
        File losedir = new File("results/gamefiles/lose");
        if (!losedir.exists())
          losedir.mkdirs();
        String gamename = "2_p_" + gameinfo.filename + "_" + APassgn + (encoded?"_encoded":"_naive");

        Strategy strategy = Parity3.classical(game, gameinfo);
        if (strategy == null) {
          if (game.numberOfParities == 2) {
            File f = new File("results/gamefiles/lose/"+gamename+".txt");
            game.dump(gameinfo, f);
          }
          continue;
        } else {
          File f = new File("results/gamefiles/win/"+gamename+".txt");
          game.dump(gameinfo, f);
        }

        if (!Parity3.checkBV(game, gameinfo, strategy)) {
          System.out.println("COMPUTED STRATEGY WHICH IS LOSING! file-"+filen+", APassgn-"+APassgn);
          continue;
        }

        Dataset ds = new Dataset(game, strategy);
        String dsname = (strategy.player==1?"1":"2") + "_" + strategy.objective + "_C_"
            + gameinfo.filename + "_" + APassgn + (encoded?"_encoded":"_naive");
        ds.arffFile(dsname, game, game.stateSize);
        System.out.println(dsname);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {

          writer.write(String.format("%-30s",gameinfo.filename+"_"+APassgn+"_naive"));
          writer.write(String.format("%12d",game.stateSize));
          writer.write(String.format("%6d",game.varActionP1no()));
          writer.write(String.format("%6d",game.varActionP2no()));
          writer.write(String.format("%16d",ds.numNO+ds.numYES));

          long startTime = System.nanoTime();
          DecisionTree dtplus = new DecisionTree(ds, true, true);
          long elapsedTime = System.nanoTime() - startTime;
          double timedtplus = ((double) elapsedTime) / 1000000000.0;
          int sizedtplus = dtplus.numberOfInnerNodes;
          int ladtplus = dtplus.lookaheadFiredUp;
          int heudtplus = dtplus.heuristicFiredUp;
          boolean resultdtplus = Parity3.checkDT(game, gameinfo, dtplus);
          dtplus = null;
          System.gc();

          startTime = System.nanoTime();
          DecisionTree dt = new DecisionTree(ds, true, false);
          elapsedTime = System.nanoTime() - startTime;
          double timedt = ((double) elapsedTime) / 1000000000.0;
          int sizedt = dt.numberOfInnerNodes;
          int ladt = dt.lookaheadFiredUp;
          int heudt = dt.heuristicFiredUp;
          boolean resultdt = Parity3.checkDT(game, gameinfo, dt);
          dt = null;
          System.gc();

          startTime = System.nanoTime();
          DecisionTree dtnl = new DecisionTree(ds, false, false);
          elapsedTime = System.nanoTime() - startTime;
          double timedtnl = ((double) elapsedTime) / 1000000000.0;
          int sizedtnl = dtnl.numberOfInnerNodes;
          int ladtnl = dtnl.lookaheadFiredUp;
          int heudtnl = dtnl.heuristicFiredUp;
          boolean resultdtnl = Parity3.checkDT(game, gameinfo, dtnl);
          dtnl = null;
          System.gc();

          if (!encoded) {
            startTime = System.nanoTime();
            BinaryDecisionDiagram bdd = new BinaryDecisionDiagram(ds, false, -1);
            int sizebdd = bdd.numberOfInnerNodes();
            int sizebddmax = bdd.numberOfInnerNodes();
            int sizebddmin = bdd.numberOfInnerNodes();
            double sizebddmean = 0;
            int attemptno = 10; // 1000
            for (int j=0; j<attemptno; j++) {
              bdd = new BinaryDecisionDiagram(ds, true, seedgen.nextInt(31337));
              sizebddmax = Math.max(sizebddmax, bdd.numberOfInnerNodes());
              sizebddmin = Math.min(sizebddmin, bdd.numberOfInnerNodes());
              sizebddmean += bdd.numberOfInnerNodes();
            }
            sizebddmean /= attemptno;
            elapsedTime = System.nanoTime() - startTime;
            double timebdd = ((double) elapsedTime) / 1000000000.0;
            bdd = null;
            System.gc();

            //BDD
            writer.write(String.format("%14d", sizebdd));
            writer.write(String.format("%8d", sizebddmin));
            writer.write(String.format("%10.1f", sizebddmean));
            writer.write(String.format("%8d", sizebddmax));
            writer.write(String.format("%8.1f", timebdd));
          } else {
            //Won't compute BDDs in encoded version
            writer.write(String.format("%14s", "x"));
            writer.write(String.format("%8s", "x"));
            writer.write(String.format("%10s", "x"));
            writer.write(String.format("%8s", "x"));
            writer.write(String.format("%8s", "x"));
          }

          //DT no lookahead
          writer.write(String.format("%12d", sizedtnl));
          writer.write(String.format("%8.1f", timedtnl));
          writer.write(String.format("%4d", ladtnl));
          writer.write(String.format("%4d", heudtnl));
          writer.write(" "+(resultdtnl?"W":"L"));

          //DT
          writer.write(String.format("%12d", sizedt));
          writer.write(String.format("%8.1f", timedt));
          writer.write(String.format("%4d", ladt));
          writer.write(String.format("%4d", heudt));
          writer.write(" "+(resultdt?"W":"L"));

          //DT plus chains
          writer.write(String.format("%12d", sizedtplus));
          writer.write(String.format("%8.1f", timedtplus));
          writer.write(String.format("%4d", ladtplus));
          writer.write(String.format("%4d", heudtplus));
          writer.write(" "+(resultdtplus?"W":"L"));

          writer.write(nl);

        } catch (Exception e) {
          e.printStackTrace();
          return;
        }

      }
    }
  }

  /**
   * WASH:: Datasets -> BDDs and DTs
   * @param n		Number of tanks (0 or 2..4)
   */
  public static void Rwash(int n) {
    assert(n == 0 || (n >= 2 && n <= 4)); // 0 deals with all reachability
    Random seedgen = new Random(47);

    ArrayList<String> filenames = new ArrayList<String>();
    File[] files = new File("results/datasets").listFiles();
    for (File file : files)
      if (file.isFile()) {
        if (n == 0) {
          if (file.getName().contains("wash") && file.getName().contains("1_r") && file.getName().contains("arff")) {
            if (file.getName().contains("_C_"))
              filenames.add(file.getName().replace(".arff", ""));
          }
        } else {
          if (file.getName().contains("wash_"+n) && file.getName().contains("2_s") && file.getName().contains("arff")) {
            if (file.getName().contains("_C_"))
              filenames.add(file.getName().replace(".arff", ""));
          }
        }
      }

    File directory = new File("results/reports/");
    if (!directory.exists())
      directory.mkdirs();
    File outputFile = new File("results/reports/reprWash"+(n==0?"reach":n)+".txt");
    String nl = System.getProperty("line.separator");

    try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
      writer.write(String.format("%118s","|BDD|"));
      writer.write(String.format("%30s","|DTnl|"));
      writer.write(String.format("%30s","|DT|"));
      writer.write(String.format("%30s","|DT+|"));
      writer.write(nl);

      writer.write(String.format("%-30s","Name"));
      writer.write(String.format("%12s","|S|"));
      writer.write(String.format("%6s","|I|"));
      writer.write(String.format("%6s","|O|"));
      writer.write(String.format("%16s","|Good|+|Bad|"));

      writer.write(String.format("%14s","Orig"));
      writer.write(String.format("%8s","Min"));
      writer.write(String.format("%10s","Mean"));
      writer.write(String.format("%8s","Max"));
      writer.write(String.format("%8s","Time"));

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(String.format("%12s", "Size"));
      writer.write(String.format("%8s", "Time"));
      writer.write(String.format("%4s", "LA"));
      writer.write(String.format("%4s", "Heu"));
      writer.write(" ?");

      writer.write(nl);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    for (int i=0; i<filenames.size(); i++) {

      GameInfo gameinfo = new GameInfo();
      gameinfo.type = 'w';
      gameinfo.n = Character.getNumericValue(filenames.get(i).charAt(11));
      gameinfo.d = Character.getNumericValue(filenames.get(i).charAt(13));
      gameinfo.k = Character.getNumericValue(filenames.get(i).charAt(15));
      gameinfo.t = Character.getNumericValue(filenames.get(i).charAt(17));
      gameinfo.lightmode = filenames.get(i).charAt(19) == 't';
      gameinfo.filename = filenames.get(i).substring(6);

      int distance = -1;
      try (Scanner sc = new Scanner(new File("results/reports/reportwash"+gameinfo.n+".txt"))) {
        String token = "";
        while (sc.hasNext()) {
          token = sc.next();
          if (token.equals(gameinfo.filename)) {
            while (!token.equals("Distance:"))
              token = sc.next();
            token = sc.next();
            token = token.substring(0, token.length()-1);
            distance = Integer.parseInt(token);
            break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      if (distance == -1) {
        System.out.println("Could not find distance for Wash game");
        return;
      }

      Pair<Game,Boolean> game = Wash.create(null, gameinfo, distance);
      assert(game.second());

      System.out.println(filenames.get(i));
      try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {

        Dataset ds = new Dataset(filenames.get(i), writer);

        long startTime = System.nanoTime();
        DecisionTree dtplus = new DecisionTree(ds, true, true);
        long elapsedTime = System.nanoTime() - startTime;
        double timedtplus = ((double) elapsedTime) / 1000000000.0;
        if (n==0 || n==2) dtplus.dotFile("dt+_"+filenames.get(i));
        int sizedtplus = dtplus.numberOfInnerNodes;
        int ladtplus = dtplus.lookaheadFiredUp;
        int heudtplus = dtplus.heuristicFiredUp;
        boolean resultdtplus = (n==0?Reachability.checkDT(game.first(), gameinfo, dtplus):
            Safety.checkDT(game.first(), gameinfo, dtplus));
        dtplus = null;
        System.gc();

        startTime = System.nanoTime();
        DecisionTree dt = new DecisionTree(ds, true, false);
        elapsedTime = System.nanoTime() - startTime;
        double timedt = ((double) elapsedTime) / 1000000000.0;
        int sizedt = dt.numberOfInnerNodes;
        int ladt = dt.lookaheadFiredUp;
        int heudt = dt.heuristicFiredUp;
        boolean resultdt = (n==0?Reachability.checkDT(game.first(), gameinfo, dt):
            Safety.checkDT(game.first(), gameinfo, dt));
        dt = null;
        System.gc();

        startTime = System.nanoTime();
        DecisionTree dtnl = new DecisionTree(ds, false, false);
        elapsedTime = System.nanoTime() - startTime;
        double timedtnl = ((double) elapsedTime) / 1000000000.0;
        int sizedtnl = dtnl.numberOfInnerNodes;
        int ladtnl = dtnl.lookaheadFiredUp;
        int heudtnl = dtnl.heuristicFiredUp;
        boolean resultdtnl = (n==0?Reachability.checkDT(game.first(), gameinfo, dtnl):
            Safety.checkDT(game.first(), gameinfo, dtnl));
        dtnl = null;
        System.gc();

        startTime = System.nanoTime();
        BinaryDecisionDiagram bdd = new BinaryDecisionDiagram(ds, false, -1);
        int sizebdd = bdd.numberOfInnerNodes();
        int sizebddmax = bdd.numberOfInnerNodes();
        int sizebddmin = bdd.numberOfInnerNodes();
        double sizebddmean = 0;
        int attemptno = 10; // 1000
        for (int j=0; j<attemptno; j++) {
          bdd = new BinaryDecisionDiagram(ds, true, seedgen.nextInt(31337));
          sizebddmax = Math.max(sizebddmax, bdd.numberOfInnerNodes());
          sizebddmin = Math.min(sizebddmin, bdd.numberOfInnerNodes());
          sizebddmean += bdd.numberOfInnerNodes();
        }
        sizebddmean /= attemptno;
        elapsedTime = System.nanoTime() - startTime;
        double timebdd = ((double) elapsedTime) / 1000000000.0;
        bdd = null;
        System.gc();

        //BDD
        writer.write(String.format("%14d", sizebdd));
        writer.write(String.format("%8d", sizebddmin));
        writer.write(String.format("%10.1f", sizebddmean));
        writer.write(String.format("%8d", sizebddmax));
        writer.write(String.format("%8.1f", timebdd));

        //DT no lookahead
        writer.write(String.format("%12d", sizedtnl));
        writer.write(String.format("%8.1f", timedtnl));
        writer.write(String.format("%4d", ladtnl));
        writer.write(String.format("%4d", heudtnl));
        writer.write(" "+(resultdtnl?"W":"L"));

        //DT
        writer.write(String.format("%12d", sizedt));
        writer.write(String.format("%8.1f", timedt));
        writer.write(String.format("%4d", ladt));
        writer.write(String.format("%4d", heudt));
        writer.write(" "+(resultdt?"W":"L"));

        //DT plus chains
        writer.write(String.format("%12d", sizedtplus));
        writer.write(String.format("%8.1f", timedtplus));
        writer.write(String.format("%4d", ladtplus));
        writer.write(String.format("%4d", heudtplus));
        writer.write(" "+(resultdtplus?"W":"L"));

        writer.write(nl);

      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      game = null;
      gameinfo = null;
      System.gc();
    }
  }

  /**
   * WASH - Games -> Datasets
   * @param n		Number of tanks (2..4)
   */
  public static void wash(int n) {

    boolean[] lightmode = {false, true};
    File directory = new File("results/reports/");
    if (!directory.exists())
      directory.mkdirs();
    File outputFile = new File("results/reports/reportwash"+n+".txt");
    String nl = System.getProperty("line.separator");
    GameInfo gameinfo = new GameInfo();
    gameinfo.type = 'w';

    int dl = 9;
    int kl = 4; if (n == 4) kl = 1;

    for (int d=1; d<=dl; d++)
      for (int k=1; k<=Math.min(kl, d); k++)
        for (int t=1; t<=Math.min(4, n); t++)
          for (int lm=0; lm<lightmode.length; lm++) {
            gameinfo.fill(n, d, k, t, lightmode[lm]);
            String filename = gameinfo.write();

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
              System.out.print(filename+"   ");
              writer.write(filename+"   ");
              int estimate = Wash.create(gameinfo, false);
              System.out.println(" expected state space size: "+estimate);
              writer.write(" expected state space size: "+estimate+nl);

              Triplet<Game, Integer, Boolean> result = Wash.create(gameinfo, writer);
              assert(result != null);
              if (result.first() == null || result.second() == -1) {
                System.gc();
                continue;
              }

              Game game = result.first();

              System.out.println("Distance: "+result.second()+", "
                  +(result.third()?"reachability":"safety")+", generated states: "+game.stateSize);
              writer.write("Distance: "+result.second()+", "
                  +(result.third()?"reachability":"safety")+", generated states: "+game.stateSize+nl);

              if (result.third()) {
                // REACHABILITY
                Strategy s = Reachability.classical(game, gameinfo);

                Dataset ds = new Dataset(game, s);
                String dsname = (s.player==1?"1":"2") + "_" + s.objective + "_C_" + filename;
                ds.arffFile(dsname, game, estimate);

                System.out.print("Classical strategy saved in "+dsname+".arff... ");
                writer.write("Classical strategy saved in "+dsname+".arff... ");
                boolean res = Reachability.check(game, gameinfo, dsname);
                System.out.println(res?"and checked successfully":"ERROR - IT'S NOT WINNING");
                writer.write((res?"and checked successfully":"ERROR - IT'S NOT WINNING")+nl);

                s = null;
                ds = null;
                System.gc();

              } else {
                // SAFETY
                Strategy s = Safety.classical(game, gameinfo);

                Dataset ds = new Dataset(game, s);
                String dsname = (s.player==1?"1":"2") + "_" + s.objective + "_C_" + filename;
                ds.arffFile(dsname, game, estimate);

                System.out.print("Classical strategy saved in "+dsname+".arff... ");
                writer.write("Classical strategy saved in "+dsname+".arff... ");
                boolean res = Safety.check(game, gameinfo, dsname);
                System.out.println(res?"and checked successfully":"ERROR - IT'S NOT WINNING");
                writer.write((res?"and checked successfully":"ERROR - IT'S NOT WINNING")+nl);

                s = null;
                ds = null;
                System.gc();

              }
              System.out.println("FINISHED\n");
              writer.write("FINISHED"+nl+nl);

            } catch (Exception e) {
              e.printStackTrace();
              // dont return, just continue to the next example
            }
          }
  }

  /**
   * AIGER:: Datasets -> BDDs and DTs
   */
  public static void Raiger() {

    ArrayList<String> filenamesC = new ArrayList<String>();
    File[] files = new File("results/datasets").listFiles();

    for (File file : files)
      if (file.isFile()) {
        if (file.getName().contains("bs") && file.getName().contains("arff"))
          if (file.getName().contains("_C_"))
            filenamesC.add(file.getName().replace(".arff", ""));
      }

    File directory = new File("results/reports/");
    if (!directory.exists())
      directory.mkdirs();
    File outputFileC = new File("results/reports/reprAiger.txt");
    String nl = System.getProperty("line.separator");
    try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileC, false))) {
      writer.write(String.format("%-30s","Name"));
      writer.write(String.format("%12s","|S|"));
      writer.write(String.format("%6s","|I|"));
      writer.write(String.format("%6s","|O|"));
      writer.write(String.format("%16s","|Good|+|Bad|"));
      writer.write(String.format("%16s","|BDD|"));
      writer.write(String.format("%24s","|DT|"));   //dt1
      writer.write(String.format("%24s","|DT+|"));  //dt2
      writer.write(nl);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    for (int i=0; i<filenamesC.size(); i++) {

      GameInfo gameinfo = new GameInfo();
      gameinfo.type = 'a'; gameinfo.filename = filenamesC.get(i).substring(6);
      Pair<Game,Boolean> game = Aiger.create(null, gameinfo.filename, 4);
      assert(game.second());

      System.out.println(filenamesC.get(i));
      try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileC, true))) {

        Dataset ds = new Dataset(filenamesC.get(i), writer);

        long startTime = System.nanoTime();
        DecisionTree dt2 = new DecisionTree(ds, true, true);
        long elapsedTime = System.nanoTime() - startTime;
        double timedt2 = ((double) elapsedTime) / 1000000000.0;
        dt2.dotFile("dt+_"+filenamesC.get(i));
        int sizedt2 = dt2.numberOfInnerNodes;
        int ladt2 = dt2.lookaheadFiredUp;
        int heudt2 = dt2.heuristicFiredUp;
        boolean resultdt2 = Safety.checkDT(game.first(), gameinfo, dt2);
        dt2 = null;
        System.gc();

        startTime = System.nanoTime();
        DecisionTree dt1 = new DecisionTree(ds, true, false);
        elapsedTime = System.nanoTime() - startTime;
        double timedt1 = ((double) elapsedTime) / 1000000000.0;
        dt1.dotFile("dt-_"+filenamesC.get(i));
        int sizedt1 = dt1.numberOfInnerNodes;
        int ladt1 = dt1.lookaheadFiredUp;
        int heudt1 = dt1.heuristicFiredUp;
        boolean resultdt1 = Safety.checkDT(game.first(), gameinfo, dt1);
        dt1 = null;
        System.gc();

        startTime = System.nanoTime();
        BinaryDecisionDiagram bdd = new BinaryDecisionDiagram(ds, false, -1);
        elapsedTime = System.nanoTime() - startTime;
        double timebdd = ((double) elapsedTime) / 1000000000.0;
        bdd.dotFile("bdd_"+filenamesC.get(i));
        int sizebdd = bdd.numberOfInnerNodes();
        bdd = null;
        System.gc();

        //BDD
        writer.write(String.format("%8d", sizebdd));
        writer.write(String.format("%8.2f", timebdd));

        //DT1
        writer.write(String.format("%8d", sizedt1));
        writer.write(String.format("%8.2f", timedt1));
        writer.write(String.format("%3d", ladt1));
        writer.write(String.format("%3d", heudt1));
        writer.write(" "+(resultdt1?"W":"L"));

        //DT2
        writer.write(String.format("%8d", sizedt2));
        writer.write(String.format("%8.2f", timedt2));
        writer.write(String.format("%3d", ladt2));
        writer.write(String.format("%3d", heudt2));
        writer.write(" "+(resultdt2?"W":"L"));

        writer.write(nl);

      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      game = null;
      gameinfo = null;
      System.gc();
    }
  }

  /**
   * AIGER:: Games -> Datasets
   */
  public static void aiger() {
    String[] files = {"bs16n", "bs32n", "bs64n"}; //, "bs128n", "bs256n", "bs512n"};

    for (int i=0; i<files.length; i++) {
      System.gc();
      String filename = files[i];

      GameInfo gameinfo = new GameInfo();
      gameinfo.type = 'a';

      System.out.print("FILE: "+filename);
      int estimate = Aiger.create(filename, false);
      System.out.println(" expected state space size: "+estimate);

      Pair<Game, Integer> result = Aiger.create(filename);
      assert(result != null);
      assert(result.first() != null & result.second() > -1);
      Game game = result.first();

      System.out.println("Distance: "+result.second()+", generated states: "+game.stateSize);

      Strategy s = Safety.classical(game, gameinfo);

      Dataset ds = new Dataset(game, s);
      String dsname = (s.player==1?"1":"2") + "_" + s.objective + "_C_" + filename;
      ds.arffFile(dsname, game, estimate);

      System.out.print("Classical strategy saved in "+dsname+".arff... ");
      System.out.println((Safety.check(game, gameinfo, dsname))?"and checked successfully":"ERROR - IT'S NOT WINNING");

      s = null;
      ds = null;
      System.out.println("FINISHED\n");
    }
  }

}

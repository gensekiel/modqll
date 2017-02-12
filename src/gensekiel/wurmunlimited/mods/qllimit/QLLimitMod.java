package gensekiel.wurmunlimited.mods.qllimit;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.CtPrimitiveType;
import javassist.Modifier;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

public class QLLimitMod implements
	WurmServerMod,
	Initable,
	Configurable
{
//======================================================================
	private static Logger logger = Logger.getLogger(QLLimitMod.class.getName());
//======================================================================
	private static double a = 1.0;
	private static double b = 0.0;
	private static boolean exact = false;
	private static double hardlimit = 99.0;
	private static boolean debug = false;
	private static boolean nesting = true;
//======================================================================
	private static boolean limitForage = true;
	private static boolean limitBotanize = true;
	private static boolean limitDig = true;
	private static boolean limitFlowers = true;
	private static boolean limitFarmHarvest = true;
	private static boolean limitSprouts = true;
	private static boolean limitTreeHarvest = true;
	private static boolean limitBushHarvest = true;
	private static boolean limitTreeChop = true;
	private static boolean limitTreeGrass = true;
	private static boolean limitGrass = true;
	private static boolean limitMineSurface = true;
	private static boolean limitMine = true;
//	private static boolean limitDisintegrate = true;
	private static boolean limitFish = true;
//======================================================================
	private static String qlform;
	private static String generic;
	private static String reset;
//======================================================================
	private static void augmentMethod(CtClass ctclazz, String method, int skillid) throws Exception
	{
		augmentMethod(ctclazz, method, skillid, null);
	}
	private static void augmentMethod(CtClass ctclazz, String method, int skillid, CtClass[] params) throws Exception
	{
		String command = "double skl = performer.getSkills().getSkillOrLearn(" + skillid + ").knowledge;" + generic;
		CtMethod ctm = null;
		if(params == null) ctm = ctclazz.getDeclaredMethod(method);
		else ctm = ctclazz.getDeclaredMethod(method, params);
		ctm.insertBefore(command);
		ctm.insertAfter(reset, true);
	}
//======================================================================
	@Override
	public void init()
	{
		try{
			qlform = "" + a + " * skl + " + b;
			generic = "ItemFactory.modSetQualityLimit(" + qlform + ");";
			reset = "ItemFactory.modResetQualityLimit();";
			String qlset0 = "ItemFactory.modSetQualityLimit(0.0);";

			if(debug){
				generic += "performer.getCommunicator().sendNormalServerMessage(\"QL limit set to \" + ItemFactory.modGetQualityLimit() + \".\");";
				reset += "performer.getCommunicator().sendNormalServerMessage(\"QL limit reset to \" + ItemFactory.modGetQualityLimit() + \".\");";
				qlset0 += "performer.getCommunicator().sendNormalServerMessage(\"QL limit set to \" + ItemFactory.modGetQualityLimit() + \".\");";
			}

			String qllimit = null;
			if(exact) qllimit = "if(modQualityLimit > 0.0) qualityLevel = modQualityLimit;";
			else qllimit = "if(modQualityLimit > 0.0) qualityLevel = (modQualityLimit < qualityLevel) ? modQualityLimit : qualityLevel;";
			qllimit += "qualityLevel = (qualityLevel > " + hardlimit + ") ? " + hardlimit + " : qualityLevel;";
			qllimit += "qualityLevel = (qualityLevel < 1.0) ? 1.0 : qualityLevel;";
			
			ClassPool pool = ClassPool.getDefault();
			pool.importPackage("com.wurmonline.server.items.ItemFactory");

			CtClass ctItemFactory = pool.get("com.wurmonline.server.items.ItemFactory");

			CtField ctQualityLimit = new CtField(CtClass.doubleType, "modQualityLimit", ctItemFactory);
			ctQualityLimit.setModifiers(Modifier.STATIC);
			ctItemFactory.addField(ctQualityLimit, "0.0");

			if(nesting){
				CtClass ctStack = pool.get("java.util.Stack");
				
				CtField ctQLStack = new CtField(ctStack, "modQLStack", ctItemFactory);
				ctQLStack.setModifiers(Modifier.STATIC);
				ctItemFactory.addField(ctQLStack, CtField.Initializer.byNew(ctStack));

				CtMethod ctSetQualityLimit = CtNewMethod.make("public static void modSetQualityLimit(double d){ modQLStack.push(new Double(modQualityLimit)); modQualityLimit = d; }", ctItemFactory);
				ctItemFactory.addMethod(ctSetQualityLimit);

				CtMethod ctResetQualityLimit = CtNewMethod.make("public static void modResetQualityLimit(){ try{ modQualityLimit = ((Double)modQLStack.pop()).doubleValue(); }catch(Exception e){ modQualityLimit = 0.0; } }", ctItemFactory);
				ctItemFactory.addMethod(ctResetQualityLimit);

				CtMethod ctGetQualityLimit = CtNewMethod.make("public static double modGetQualityLimit(){ return modQualityLimit; }", ctItemFactory);
				ctItemFactory.addMethod(ctGetQualityLimit);
			}else{
				CtMethod ctSetQualityLimit = CtNewMethod.make("public static void modSetQualityLimit(double d){ modQualityLimit = d; }", ctItemFactory);
				ctItemFactory.addMethod(ctSetQualityLimit);

				CtMethod ctResetQualityLimit = CtNewMethod.make("public static void modResetQualityLimit(){ modQualityLimit = 0.0; }", ctItemFactory);
				ctItemFactory.addMethod(ctResetQualityLimit);

				CtMethod ctGetQualityLimit = CtNewMethod.make("public static double modGetQualityLimit(){ return modQualityLimit; }", ctItemFactory);
				ctItemFactory.addMethod(ctGetQualityLimit);
			}

			CtClass ctString = pool.get("java.lang.String");

			CtMethod ctCreateItem = ctItemFactory.getDeclaredMethod("createItem", new CtClass[]{
				CtPrimitiveType.intType, CtPrimitiveType.floatType, CtPrimitiveType.byteType, CtPrimitiveType.byteType, 
				CtPrimitiveType.longType, ctString
			});
			ctCreateItem.insertBefore(qllimit);

			CtMethod ctCreateItem2 = ctItemFactory.getDeclaredMethod("createItem", new CtClass[]{
				CtPrimitiveType.intType, CtPrimitiveType.floatType, CtPrimitiveType.floatType, CtPrimitiveType.floatType, 
				CtPrimitiveType.floatType, CtPrimitiveType.booleanType, CtPrimitiveType.byteType, CtPrimitiveType.byteType, 
				CtPrimitiveType.longType, ctString, CtPrimitiveType.byteType
			});
			ctCreateItem2.insertBefore(qllimit);

			CtClass ctTileBehaviour = pool.get("com.wurmonline.server.behaviours.TileBehaviour");

			if(limitForage){
				augmentMethod(ctTileBehaviour, "forage",    10071);
				augmentMethod(ctTileBehaviour, "forageV11", 10071);
			}
			if(limitBotanize) augmentMethod(ctTileBehaviour, "botanizeV11", 10072);

			CtClass ctTerraforming = pool.get("com.wurmonline.server.behaviours.Terraforming");

			if(limitDig)         augmentMethod(ctTerraforming, "dig",               1009);
			if(limitFlowers)     augmentMethod(ctTerraforming, "pickFlower",       10045);
			if(limitFarmHarvest) augmentMethod(ctTerraforming, "harvest",          10049);
			if(limitSprouts)     augmentMethod(ctTerraforming, "pickSprout",       10048);
			if(limitTreeHarvest) augmentMethod(ctTerraforming, "harvestTree",      10048);
			if(limitBushHarvest) augmentMethod(ctTerraforming, "harvestBush",      10048);
			if(limitTreeChop)    augmentMethod(ctTerraforming, "handleChopAction",  1007);

			CtClass ctTileTreeBehaviour = pool.get("com.wurmonline.server.behaviours.TileTreeBehaviour");

			if(limitTreeGrass) augmentMethod(ctTileTreeBehaviour, "cutGrass", 10045);

			CtClass ctTileGrassBehaviour = pool.get("com.wurmonline.server.behaviours.TileGrassBehaviour");

			if(limitGrass) augmentMethod(ctTileGrassBehaviour, "cutGrass", 10045);

			CtClass ctTileRockBehaviour = pool.get("com.wurmonline.server.behaviours.TileRockBehaviour");
			CtClass ctAction = pool.get("com.wurmonline.server.behaviours.Action");
			CtClass ctCreature = pool.get("com.wurmonline.server.creatures.Creature");
			CtClass ctItem = pool.get("com.wurmonline.server.items.Item");

			if(limitMineSurface) augmentMethod(ctTileRockBehaviour, "action", 1008, new CtClass[]{
				ctAction, ctCreature, ctItem, CtPrimitiveType.intType, CtPrimitiveType.intType, CtPrimitiveType.booleanType, 
				CtPrimitiveType.intType, CtPrimitiveType.intType, CtPrimitiveType.shortType, CtPrimitiveType.floatType
			});

			CtClass ctCaveWallBehaviour = pool.get("com.wurmonline.server.behaviours.CaveWallBehaviour");
			CtClass ctCaveTileBehaviour = pool.get("com.wurmonline.server.behaviours.CaveTileBehaviour");

			if(limitMine){
				augmentMethod(ctCaveWallBehaviour, "action", 1008, new CtClass[]{
					ctAction, ctCreature, ctItem, CtPrimitiveType.intType, CtPrimitiveType.intType, CtPrimitiveType.booleanType,
					CtPrimitiveType.intType, CtPrimitiveType.intType, CtPrimitiveType.shortType, CtPrimitiveType.floatType
				});
				augmentMethod(ctCaveTileBehaviour, "handle_MINE", 1008);
				augmentMethod(ctCaveTileBehaviour, "flatten", 1008);
			}

//			CtClass ctDisintegrate = pool.get("com.wurmonline.server.spells.Disintegrate");
//			
//			if(limitDisintegrate) augmentMethod(ctDisintegrate, "doEffect", 1008);

			CtClass ctFish = pool.get("com.wurmonline.server.behaviours.Fish");

			if(limitFish) augmentMethod(ctFish, "fish", 10033);

			if(nesting){
				// Protect some methods
				if(limitMine || limitMineSurface){
					CtMethod ctm = ctTileRockBehaviour.getDeclaredMethod("maybeCreateSource");
					ctm.insertBefore(qlset0);
					ctm.insertAfter(reset, true);
				}
			}

		}catch(Exception e){
			logger.log(Level.WARNING, "Setup failed, QL limit mod will not work. Exception: " + e);
		}
	}
//======================================================================
	private Double getOption(String option, Double default_value, Properties properties){ return Double.valueOf(properties.getProperty(option, Double.toString(default_value))); }
	private Boolean getOption(String option, Boolean default_value, Properties properties){ return Boolean.valueOf(properties.getProperty(option, Boolean.toString(default_value))); }
//======================================================================
	@Override
	public void configure(Properties properties)
	{
		a = getOption("a", a, properties);
		b = getOption("b", b, properties);
		exact = getOption("exact", exact, properties);
		hardlimit = getOption("hardlimit", hardlimit, properties);
		debug = getOption("debug", debug, properties);
		nesting = getOption("nesting", nesting, properties);
		limitForage = getOption("limitForage", limitForage, properties);
		limitBotanize = getOption("limitBotanize", limitBotanize, properties);
		limitDig = getOption("limitDig", limitDig, properties);
		limitFlowers = getOption("limitFlowers", limitFlowers, properties);
		limitFarmHarvest = getOption("limitFarmHarvest", limitFarmHarvest, properties);
		limitSprouts = getOption("limitSprouts", limitSprouts, properties);
		limitTreeHarvest = getOption("limitTreeHarvest", limitTreeHarvest, properties);
		limitBushHarvest = getOption("limitBushHarvest", limitBushHarvest, properties);
		limitTreeChop = getOption("limitTreeChop", limitTreeChop, properties);
		limitTreeGrass = getOption("limitTreeGrass", limitTreeGrass, properties);
		limitGrass = getOption("limitGrass", limitGrass, properties);
		limitMineSurface = getOption("limitMineSurface", limitMineSurface, properties);
		limitMine = getOption("limitMine", limitMine, properties);
//		limitDisintegrate = getOption("limitDisintegrate", limitDisintegrate, properties);
		limitFish = getOption("limitFish", limitFish, properties);
	}
//======================================================================
}

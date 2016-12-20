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
	private static double a = 1.0;
	private static double b = 0.0;
	private static boolean exact = false;
	private static double hardlimit = 99.0;
//======================================================================
	@Override
	public void init()
	{
		try{
			String generic = "ItemFactory.modSetQualityLimit(" + a + " * skl + " + b + ");";
			String forage = "double skl = performer.getSkills().getSkillOrLearn(10071).knowledge;" + generic;
			String botanize = "double skl = performer.getSkills().getSkillOrLearn(10072).knowledge;" + generic;
			String reset = "ItemFactory.modSetQualityLimit(0.0);";
			
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
			ctItemFactory.addField(ctQualityLimit);
	
			CtMethod ctSetQualityLimit = CtNewMethod.make("public static void modSetQualityLimit(double d){ modQualityLimit = d; }", ctItemFactory);
			ctItemFactory.addMethod(ctSetQualityLimit);
	
			CtMethod ctGetQualityLimit = CtNewMethod.make("public static double modGetQualityLimit(){ return modQualityLimit; }", ctItemFactory);
			ctItemFactory.addMethod(ctGetQualityLimit);
	
			CtClass ctString = pool.get("java.lang.String");
			CtMethod ctCreateItem = ctItemFactory.getDeclaredMethod("createItem", new CtClass[]{
				CtPrimitiveType.intType, CtPrimitiveType.floatType, CtPrimitiveType.byteType, CtPrimitiveType.byteType, ctString
			});
			ctCreateItem.insertBefore(qllimit);
			
			CtClass ctTileBehaviour = pool.get("com.wurmonline.server.behaviours.TileBehaviour");

			CtMethod ctForage = ctTileBehaviour.getDeclaredMethod("forage");
			ctForage.insertBefore(forage);
			ctForage.insertAfter(reset, true);
	
			CtMethod ctForageV11 = ctTileBehaviour.getDeclaredMethod("forageV11");
			ctForageV11.insertBefore(forage);
			ctForageV11.insertAfter(reset, true);

			CtMethod ctBotanizeV11 = ctTileBehaviour.getDeclaredMethod("botanizeV11");
			ctBotanizeV11.insertBefore(botanize);
			ctBotanizeV11.insertAfter(reset, true);
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
	}
//======================================================================
}

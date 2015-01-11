package battlebot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotPlayer 
{
	static Direction _facing;
	static Random _rand;
	static RobotController _rc;
	
	public static void run(RobotController rc) 
	{
		_rc = rc;
		_rand = new Random(rc.getID());
		_facing = getRandomDirection(); //randomize starting direction
		
		while(true)
		{
			try 
			{
				if(rc.getType() == RobotType.HQ)
				{
					attackEnemyZero(rc);
					spawnUnit(RobotType.BEAVER);
				}
				else if (rc.getType() == RobotType.BEAVER)
				{
					attackEnemyZero(rc);
					if (Clock.getRoundNum() < 700)
						buildUnit(RobotType.MINERFACTORY);
					else
						buildUnit(RobotType.BARRACKS);
						
					mineAndMove();
				}
				else if (rc.getType() == RobotType.MINER)
				{
					attackEnemyZero(rc);
					mineAndMove();
				}
				else if (rc.getType() == RobotType.MINERFACTORY)
				{
					spawnUnit(RobotType.MINER);
				}
				else if (rc.getType() == RobotType.BARRACKS)
				{
					spawnUnit(RobotType.SOLDIER);
				}
				else if (rc.getType() == RobotType.TOWER)
				{
					attackEnemyZero(rc);
				}
				else if (rc.getType() == RobotType.SOLDIER)
				{
					attackEnemyZero(rc);
					moveAround();
				}
				
				TransferSupplies();			
				
			}
			catch (GameActionException e) 
			{
				e.printStackTrace();
			}	
			
			rc.yield();
		}
	}

	private static void TransferSupplies()
			throws GameActionException 
	{
		RobotInfo[] nearbyAllies = _rc.senseNearbyRobots(_rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, _rc.getTeam());
		
		double lowestSupply = _rc.getSupplyLevel();
		
		for(RobotInfo r: nearbyAllies)
		{
			double transferAmount = 0;
			MapLocation suppliesToThisLocation = null;
			if(r.supplyLevel < lowestSupply)
			{
				lowestSupply = r.supplyLevel;
				transferAmount = (_rc.getSupplyLevel() - r.supplyLevel) / 2;
				suppliesToThisLocation = r.location;
			}
			
			if (suppliesToThisLocation != null)
			{
				_rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
			}
		}
	}

	private static void buildUnit(RobotType type)
			throws GameActionException 
	{
		if(_rc.getTeamOre() > type.oreCost)
		{
			Direction buildDir = getRandomDirection();
			if(_rc.isCoreReady() && _rc.canBuild(buildDir, type))
			{
				_rc.build(buildDir, type);
			}
		}
	}

	private static void attackEnemyZero(RobotController rc)
			throws GameActionException 
	{
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		
		if(nearbyEnemies.length > 0)
		{
			if (rc.isWeaponReady() && rc.canAttackLocation(nearbyEnemies[0].location))
			{
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
	}
	
	private static void spawnUnit(RobotType type) throws GameActionException
	{
		Direction dir = getRandomDirection();
		
		if(_rc.isCoreReady() && _rc.canSpawn(dir, type))
		{						
				_rc.spawn(dir,type);						
		}
	}

	private static Direction getRandomDirection() {
		return Direction.values()[(int)_rand.nextDouble() * 8];
	}
	
	private static void mineAndMove() throws GameActionException
	{
		if(_rc.senseOre(_rc.getLocation()) > 1)
		{
			if (_rc.isCoreReady() && _rc.canMine())
			{
				_rc.mine();
			}
		}
		else
		{
			moveAround();
		}	
	}
	
	private static void moveAround() throws GameActionException
	{
		if (_rand.nextDouble() < 0.05)
		{
			if (_rand.nextDouble() < 0.05)
			{
				_facing = _facing.rotateLeft();
			}
			else 
			{
				_facing = _facing.rotateRight();							
			}
		}
		
		MapLocation tileInFront = _rc.getLocation().add(_facing);
		
		MapLocation[] enemyTowers = _rc.senseEnemyTowerLocations();
		
		boolean tileInFrontSafe = true;
		
		for(MapLocation m: enemyTowers)
		{
			if (m.distanceSquaredTo(tileInFront) <= RobotType.TOWER.attackRadiusSquared)
			{
				tileInFrontSafe = false;
				break;
			}
		}
		
		if (_rc.senseTerrainTile( tileInFront) != TerrainTile.NORMAL || !tileInFrontSafe)
		{
			_facing = _facing.rotateLeft();			
		}
		
		if (_rc.senseTerrainTile( tileInFront) != TerrainTile.NORMAL || !tileInFrontSafe)
		{
			_facing = _facing.rotateRight();
		}		

		if(_rc.isCoreReady() && _rc.canMove(_facing))
		{
			_rc.move(_facing);
		}
	}

}
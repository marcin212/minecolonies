package com.minecolonies.colony.workorders;

import com.minecolonies.MineColonies;
import com.minecolonies.colony.CitizenData;
import com.minecolonies.colony.Colony;
import com.minecolonies.colony.jobs.Job;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class WorkOrder
{
    protected UUID id;
    protected UUID claimedBy;

    //  Job and View Class Mapping
    private static Map<String, Class<? extends WorkOrder>> nameToClassMap = new HashMap<String, Class<? extends WorkOrder>>();
    private static Map<Class<? extends WorkOrder>, String> classToNameMap = new HashMap<Class<? extends WorkOrder>, String>();

    private static final String TAG_TYPE = "type";
    private static final String TAG_ID   = "id";
    private static final String TAG_CLAIMED_BY = "claimedBy";

    static
    {
        addMapping("build", WorkOrderBuild.class);
    }

    /**
     * Add a given Work Order mapping
     *
     * @param name       name of work order
     * @param orderClass class of work order
     */
    private static void addMapping(String name, Class<? extends WorkOrder> orderClass)
    {
        if (nameToClassMap.containsKey(name))
        {
            throw new IllegalArgumentException("Duplicate type '" + name + "' when adding Work Order class mapping");
        }

        try
        {
            if (orderClass.getDeclaredConstructor() != null)
            {
                nameToClassMap.put(name, orderClass);
                classToNameMap.put(orderClass, name);
            }
        }
        catch (NoSuchMethodException exception)
        {
            throw new IllegalArgumentException("Missing constructor for type '" + name + "' when adding Work Order class mapping");
        }
    }

    /**
     * Default constructor; we also start with a new id and replace it during loading;
     * this greatly simplifies creating subclasses
     */
    public WorkOrder()
    {
        id = UUID.randomUUID();
    }

    /**
     * Get the ID of the Work Order
     * @return uuid of the work order
     */
    public UUID getID()
    {
        return id;
    }

    /**
     * Is the Work Order claimed?
     * @return true if the Work Order has been claimed
     */
    public boolean isClaimed()
    {
        return claimedBy != null;
    }

    /**
     * Is the Work Order claimed by the given citizen?
     * @param citizen The citizen to check
     * @return true if the Work Order is claimed by this Citizen
     */
    public boolean isClaimedBy(CitizenData citizen)
    {
        return citizen.getId().equals(claimedBy);
    }

    /**
     * Get the UUID of the Citizen that the Work Order is claimed by
     * @return uuid of citizen the Work Order has been claimed by, or null
     */
    public UUID getClaimedBy()
    {
        return claimedBy;
    }

    /**
     * Set the Work Order as claimed by the given Citizen
     * @param citizen
     */
    public void setClaimedBy(CitizenData citizen)
    {
        claimedBy = (citizen != null) ? citizen.getId() : null;
    }

    /**
     * Clear the Claimed By status of the Work Order
     */
    public void clearClaimedBy()
    {
        claimedBy = null;
    }

    /**
     * Create a Work Order from a saved NBTTagCompound
     * @param compound the compound that contains the data for the Work Order
     * @return
     */
    public static WorkOrder createFromNBT(NBTTagCompound compound)
    {
        WorkOrder order = null;
        Class<? extends WorkOrder> oclass = null;

        try
        {
            oclass = nameToClassMap.get(compound.getString(TAG_TYPE));

            if (oclass != null)
            {
                Constructor<?> constructor = oclass.getDeclaredConstructor();
                order = (WorkOrder) constructor.newInstance();
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        if (order != null)
        {
            try
            {
                order.readFromNBT(compound);
            }
            catch (Exception ex)
            {
                MineColonies.logger.error(String.format("A WorkOrder %s(%s) has thrown an exception during loading, its state cannot be restored. Report this to the mod author", compound.getString(TAG_TYPE), oclass.getName()), ex);
                order = null;
            }
        }
        else
        {
            MineColonies.logger.warn(String.format("Unknown WorkOrder type '%s' or missing constructor of proper format.", compound.getString(TAG_TYPE)));
        }

        return order;
    }

    /**
     * Save the Work Order to an NBTTagCompound
     * @param compound
     */
    public void writeToNBT(NBTTagCompound compound)
    {
        String s = classToNameMap.get(this.getClass());

        if (s == null)
        {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        }

        compound.setString(TAG_TYPE, s);
        compound.setString(TAG_ID, id.toString());
        if (claimedBy != null)
        {
            compound.setString(TAG_CLAIMED_BY, claimedBy.toString());
        }
    }

    /**
     * Read the WorkOrder data from the NBTTagCompound
     * @param compound
     */
    public void readFromNBT(NBTTagCompound compound)
    {
        id = UUID.fromString(compound.getString(TAG_ID));
        if (compound.hasKey(TAG_CLAIMED_BY))
        {
            claimedBy = UUID.fromString(compound.getString(TAG_CLAIMED_BY));
        }
    }

    /**
     * Attempt to fulfill the Work Order.
     *
     * Override this with an implementation for the Work Order to find a Citizen to perform the job
     *
     * @param colony The colony that owns the Work Order
     */
    public void attemptToFulfill(Colony colony) {}
}

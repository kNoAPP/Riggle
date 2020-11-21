using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ShootScript : MonoBehaviour
{
    // Start is called before the first frame update

    public Transform shootLoc;
    public GameObject bulletPref;
    // Update is called once per frame
    void Update()
    {
        if(Input.GetButtonDown("Fire1"))
        {
            Shoot();
        }
    }

    void Shoot ()
    {
        Instantiate(bulletPref, shootLoc.position, shootLoc.rotation);
    }
}

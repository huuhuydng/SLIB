import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';


import 'LoginPage.dart';
import 'OTPPage.dart';


class Registerpage extends StatefulWidget{


  @override
  State<Registerpage> createState() => _RegisterpageState();
}

class _RegisterpageState extends State<Registerpage> {
  DateTime? _selectedDate;
  final TextEditingController _dateController = TextEditingController();
  bool _obscureText = true;

  @override
  void dispose() {
    _dateController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      body: Stack(
        children: [
          //1 phần nền cam
          Column(
            children: [
              Expanded(

                  flex: 3,
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.deepOrangeAccent,
                      borderRadius: BorderRadius.vertical(
                        bottom: Radius.circular(10),
                      )
                    ),
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.start,

                        children: [
                          SizedBox(height: 120,),
                          Text("Đăng Ký Tài Khoản",
                          style: TextStyle(
                            fontSize: 40,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                          ),
                          SizedBox(height: 10,),

                          RichText(
                              text: TextSpan(
                                text: "Đã có tài khoản? ",
                                style: TextStyle( fontSize: 14, color: Colors.white),
                                children: [
                                  TextSpan(
                                    text: "Đăng nhập ngay",
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontWeight: FontWeight.bold,
                                      decoration: TextDecoration.underline,
                                    ),
                                    recognizer: TapGestureRecognizer()
                                      ..onTap = () {
                                      Navigator.push(context, MaterialPageRoute(
                                          builder: (context) => Loginpage())
                                      );
                                      }
                                  )
                                ]
                          )
                          ),
                          SizedBox(height: 70,)

                        ]
                      ),
                    ),
                  )
              ),


              // 2 phần trắng
              Expanded(
                flex: 2,
                child: Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.vertical(
                      top: Radius.circular(30),
                    ),
                  ),
                ),
              ),
            ],
          ),

          //button back
          Positioned(
            top: 40,
            left: 10,
            child: IconButton(
              icon: Icon(Icons.arrow_back, color: Colors.white, size: 20),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
          ),


              // form nổi
              Positioned(
                top: MediaQuery.of(context).size.height * 0.28,
                left: 0,
                right: 0,
                child: Center(
                  child: Container(
                    child: Container(
                      width: MediaQuery.of(context).size.width * 0.95,
                      padding: EdgeInsets.all(30),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(20),
                        boxShadow: [
                        BoxShadow(
                        color: Colors.black12,
                        blurRadius: 15,
                        spreadRadius: 3,
                        offset: Offset(0, 5),
                        ),
                        ],
                    ),

                      child: Column(
                        // mainAxisSize: MainAxisSize.min,

                        children: [
                          SizedBox(height: 1,),

                          Row(
                            children: [
                              Expanded(
                                flex: 1,
                                  child: TextField(
                                    decoration: InputDecoration(
                                      labelText: "Tên",
                                      labelStyle: TextStyle(
                                        color: Colors.grey,
                                      ),
                                      border: OutlineInputBorder(
                                        borderSide: BorderSide(color: Colors.grey),
                                      ),

                                    ),
                                  )
                              ),
                              SizedBox(width: 20,),

                              Expanded(
                                  child: TextField(
                                decoration: InputDecoration(
                                  labelText: "Họ (tùy chọn)",
                                  labelStyle: TextStyle(
                                    color: Colors.grey,
                                  ),
                                  border: OutlineInputBorder(),
                                ),
                              ))
                            ],
                          ),
                          SizedBox(height: 20),

                          TextField(
                            decoration: InputDecoration(
                              labelText: "Email FPT (fpt.edu.vn)",
                              labelStyle: TextStyle(
                                color: Colors.grey,
                              ),
                              border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                            ),
                          ),
                          SizedBox(height: 20),

                          //form date
                          TextField(
                            controller: _dateController,
                            readOnly: true,
                            decoration: InputDecoration(
                              labelText: "DD/MM/YYYY",
                              labelStyle: TextStyle(
                                color: Colors.grey,
                              ),
                              border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                              suffixIcon: Icon(Icons.calendar_today,
                                color: Colors.grey,),


                            ),
                              onTap: () async {
                                DateTime? pickedDate = await showDatePicker(
                                  context: context,
                                  initialDate: DateTime.now(),
                                  firstDate: DateTime(2000),
                                  lastDate: DateTime(2100),
                                );

                                if (pickedDate != null) {
                                  setState(() {

                                    // Format DD/MM/YYYY
                                    String day = pickedDate.day.toString().padLeft(2, '0');
                                    String month = pickedDate.month.toString().padLeft(2, '0');
                                    String year = pickedDate.year.toString();

                                    _dateController.text = "$day/$month/$year";  // hoặc dùng /$yy cho ngắn
                                  });
                                }
                                // print("Chọn ngày: $pickedDate");
                              }
                          ),
                          SizedBox(height: 20,),

                          TextField(
                            decoration: InputDecoration(
                              labelText: "MSSV (Vd: De180057)",
                              labelStyle: TextStyle(
                                color: Colors.grey,
                              ),
                              border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                            ),
                          ),
                          SizedBox(height: 15),

                          TextField(
                            obscureText: _obscureText,
                            decoration: InputDecoration(
                              labelText: "Mật khẩu",
                              labelStyle: TextStyle(
                                color: Colors.grey,
                              ),
                              border: OutlineInputBorder(borderSide: BorderSide(color: Colors.grey),),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscureText ? Icons.visibility_off : Icons.visibility,
                                  color:  Colors.grey,
                                ),
                                onPressed: () {
                                  setState(() {
                                    _obscureText = !_obscureText;
                                  });
                                },
                              ),
                            ),
                          ),
                          SizedBox(height: 20),


                    // Đường kẻ
                          Row(
                            children: [
                              Expanded(
                                child: Divider(
                                  thickness: 1,
                                  color: Colors.grey,
                                ),
                              ),
                              Padding(
                                padding: EdgeInsets.symmetric(horizontal: 8),
                                child: Text("Hoặc"),
                              ),
                              Expanded(
                                child: Divider(
                                  thickness: 1,
                                  color: Colors.grey,
                                ),
                              ),
                            ],
                          ),
                          SizedBox(height: 30),

                          //DANGNHAP GG
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: () {
                                Navigator.push(context, MaterialPageRoute(builder: (context) => OTPPage()),);
                              },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white,
                                padding: EdgeInsets.symmetric(vertical: 14),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(10),
                                ),
                              ),
                              child:
                                Row(
                                    mainAxisAlignment: MainAxisAlignment.start,
                                    children: [
                                      SizedBox(width: 30,),
                                      Brand(
                                        Brands.google,
                                        size: 25,
                                      ),
                                      SizedBox(width: 60),
                                      Text("Tiếp tục với Google",
                                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                                    ],
                                )

                          )
                          )
                        ],
                      )

                  ),
                ),
              ),
              ),
        ],
      ),
    );
  }
}



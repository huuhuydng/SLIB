import 'package:flutter/material.dart';
import 'LoginPage.dart';
import 'package:flutter/gestures.dart';
import 'RegisterPage.dart';



class Homepage extends StatefulWidget{

  @override
  State<Homepage> createState() => _HomepageState();
}

class _HomepageState extends State<Homepage> {
  final PageController _pageController = PageController();
  int _currentIndex = 0;

  // Data cho onboarding
  final List<Map<String, String>> onboardingData = [
    {
      "image": "assets/images/on_boarding_1.png",
      "title": "Thư viện thông minh ngay trong tay bạn!",
      "desc": "Khám phá SmartLib – nơi kết nối và quản lý mọi hoạt động thư viện chỉ với một ứng dụng."
    },
    {
      "image": "assets/images/on_boarding_2.png",
      "title": "Check in nhanh chỉ voi 1 chạm",
      "desc": "Dùng điện thoại để vào/ra thư viện tiện lợi và hiện đại bằng công nghệ HCE."
    },
    {
      "image": "assets/images/on_boarding_3.png",
      "title": "Đặt chỗ học tập dễ dàng-chính xác từng vị trí",
      "desc": "Xem chỗ trống theo thời gian thực và đặt chỗ yêu thích chỉ trong vài giây.."
    },
    {
      "image": "assets/images/on_boarding_4.png",
      "title": "AI hỗ trợ- học tập hiệu quả hơn mỗi ngày",
      "desc": "Nhận gợi ý giờ học trên thư viện, giải đáp thắc mắc cùng chatbot AI để tối ưu hoá trải nghiệm trên thư viện của bạn."
    }
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      body: SafeArea(child: Column(
          children: [
            Expanded(
              flex: 8,
              child: PageView.builder(
                controller: _pageController,
                itemCount: onboardingData.length,
                onPageChanged: (index){
                  setState(() {
                    _currentIndex = index;
                  });

                },
                itemBuilder: ( context,index) {
                  return Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      SizedBox(height: 60,),
                      //title
                      Text(
                        onboardingData[index]["title"]!,
                        textAlign: TextAlign.center,
                        style:TextStyle( fontSize: 25,
                        fontWeight: FontWeight.bold,) ,
                      ),
                      SizedBox(height: 20,),
                      //hình
                      Image.asset(onboardingData[index]["image"]!, height: 300),
                      SizedBox(height: 100,),
                      //desc
                      Padding(padding:
                      EdgeInsets.symmetric(horizontal: 40),
                        child: Text(
                          onboardingData[index]["desc"]!,
                          textAlign: TextAlign.center,
                        ),
                      ),


                    ],
                  );
                },

              ),
            ),
            //dot
            Row(
                mainAxisAlignment: MainAxisAlignment.center,
                //tạo danh sá
                children: List.generate(onboardingData.length, (index) => AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    margin: const EdgeInsets.symmetric(horizontal: 5),
                    width: _currentIndex == index ? 20 : 8,
                    height: 8,
                    decoration: BoxDecoration(
                        color: _currentIndex == index ? Colors.blue : Colors.grey,
                        borderRadius: BorderRadius.circular(5)
                    )

                ))

            ),



            //button
            Expanded(
              flex: 3,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [

                  // BUTTON
                  SizedBox(
                    width: 300,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.deepOrangeAccent,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(builder: (context) => Registerpage()),
                        );
                      },
                      child: Text("Bắt đầu ngay",
                        style: TextStyle(fontSize: 20,  color: Colors.white,),
                      ),
                    ),
                  ),

                  SizedBox(height: 10),

                  //
                  RichText(
                    text: TextSpan(
                      text: "Bạn đã có tài khoản? ",
                      style: TextStyle( fontSize: 14, color: Colors.black),
                      children: [
                        TextSpan(
                          text: "Đăng nhập ngay",
                          style: TextStyle(
                            color: Colors.orange,
                            fontWeight: FontWeight.bold,
                          ),

                          //tạo sk click vào text
                          recognizer: TapGestureRecognizer()
                            ..onTap = () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => Loginpage(),
                                ),
                              );
                            },
                        ),


                      ],
                    ),
                  )

                ],
              ),
            ),
          ]
      ),),
    );
  }



}